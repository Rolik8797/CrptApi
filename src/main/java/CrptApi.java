import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore requestSemaphore;
    private final ScheduledExecutorService scheduler;

    private int requestLimit;
    private TimeUnit timeUnit;

    public CrptApi() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.requestSemaphore = new Semaphore(0);
        this.scheduler = Executors.newScheduledThreadPool(1);

        // По умолчанию, устанавливаем ограничение в 1 запрос в секунду
        this.requestLimit = 1;
        this.timeUnit = TimeUnit.SECONDS;

        // Запускаем задачу по периодическому освобождению разрешений
        scheduler.scheduleAtFixedRate(() -> {
            requestSemaphore.release(requestLimit);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void setRequestLimit(int requestLimit) {
        this.requestLimit = requestLimit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public void createDocument(String accessToken, Document document) {
        try {
            if (requestSemaphore.tryAcquire()) { // Попытка захвата разрешения перед запросом
                String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
                String requestBody = objectMapper.writeValueAsString(document);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Обработка ответа по необходимости
                System.out.println("API Response Code: " + response.statusCode());
                System.out.println("API Response Body: " + response.body());

            } else {
                System.err.println("Failed to acquire semaphore permit. Request limit exceeded.");
                // Обработка ситуации, когда лимит запросов превышен
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            requestSemaphore.release(); // Освобождение разрешения после завершения запроса
        }
    }


    // Внутренний класс, представляющий структуру документа
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public Document(Description description, String doc_id, String doc_status, String doc_type, boolean importRequest,
                        String owner_inn, String participant_inn, String producer_inn, String production_date,
                        String production_type, List<Product> products, String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        // Внутренний класс, представляющий "description" часть документа
        public static class Description {
            private String participantInn;

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        // Внутренний класс, представляющий "products" часть документа
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;

            public Product(String certificate_document, String certificate_document_date, String certificate_document_number,
                           String owner_inn, String producer_inn, String production_date, String tnved_code, String uit_code,
                           String uitu_code) {
                this.certificate_document = certificate_document;
                this.certificate_document_date = certificate_document_date;
                this.certificate_document_number = certificate_document_number;
                this.owner_inn = owner_inn;
                this.producer_inn = producer_inn;
                this.production_date = production_date;
                this.tnved_code = tnved_code;
                this.uit_code = uit_code;
                this.uitu_code = uitu_code;
            }
        }
    }
}