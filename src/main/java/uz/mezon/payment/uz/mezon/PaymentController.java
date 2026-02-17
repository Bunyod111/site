package uz.mezon.payment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class PaymentController {

    private static final int SHOP_ID = 42228;
    private static final String SECRET_KEY = "85d27eb9-45f0-4386-8d68-7eaea6b9bd68";

    @GetMapping("/")
    public String showPaymentPage() { return "index"; }

    @GetMapping("/offer")
    public String showOfferPage() { return "offer"; }

    @GetMapping("/success")
    public String showSuccessPage() { return "success"; }

    @PostMapping("/create-payment")
    public String createPayment(Model model) {
        try {
            // Курс ЦБ РУз (1 USD = 12199.10 UZS)
            double amount = 60995.50;
            String currency = "UZS";

            // Исправленное описание
            String description = "Mezon Math Contest ($5.00 по курсу ЦБ РУз)";

            boolean isTest = false;

            String orderId = UUID.randomUUID().toString();
            String initTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Ваш боевой домен
            String returnUrl = "https://pay.mezon.uz/success";

            String jsonBody = String.format(Locale.US,
                    "{\"octo_shop_id\":%d,\"octo_secret\":\"%s\",\"shop_transaction_id\":\"%s\",\"auto_capture\":true,\"test\":%b,\"init_time\":\"%s\",\"total_sum\":%.2f,\"currency\":\"%s\",\"description\":\"%s\",\"return_url\":\"%s\"}",
                    SHOP_ID, SECRET_KEY, orderId, isTest, initTime, amount, currency, description, returnUrl
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://secure.octo.uz/prepare_payment"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            Pattern pattern = Pattern.compile("\"octo_pay_url\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(responseBody);

            if (matcher.find()) {
                return "redirect:" + matcher.group(1);
            } else {
                model.addAttribute("message", responseBody);
                return "error";
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "System Error: " + e.getMessage());
            return "error";
        }
    }
}