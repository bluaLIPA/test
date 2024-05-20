import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class FlightAnalyzer {
    //я использовал IntelliJ IDEA, из за этого добавил испорты с помощью jar файла
    //import org.json.JSONArray;
    //import org.json.JSONObject;
    public static void main(String[] args) {
        //тут я просто проверял будет ли работать если брать tickets.json не с аргумента а с помощью path
        //String downloadsDirectory = System.getProperty("user.home") + "\\Downloads";
        //String jsonFilePath = downloadsDirectory + "\\tickets.json";

        String jsonFilePath = args[0];
        try {
            String jsonData = readFile(jsonFilePath);
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray tickets = jsonObject.getJSONArray("tickets");

            Map<String, List<JSONObject>> flightsByCarrier = groupFlightsByCarrier(tickets);

            System.out.println("Минимальное время полета между Владивостоком и Тель-Авивом:");
            for (Map.Entry<String, List<JSONObject>> entry : flightsByCarrier.entrySet()) {
                String carrier = entry.getKey();
                List<JSONObject> flights = entry.getValue();
                Optional<JSONObject> shortestFlight = flights.stream()
                        .min(Comparator.comparingInt(f -> calculateFlightDuration(f)));
                shortestFlight.ifPresent(flight -> {
                    int duration = calculateFlightDuration(flight);
                    System.out.println(carrier + ": " + duration + " мин");
                });
            }

            List<Integer> prices = flightsByCarrier.values().stream()
                    .flatMap(List::stream)
                    .map(flight -> flight.getInt("price"))
                    .collect(Collectors.toList());
            int averagePrice = (int) prices.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
            int medianPrice = calculateMedian(prices);
            System.out.println("Разница между средней ценой и медианой: " + (averagePrice - medianPrice) + " руб");

        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    private static String readFile(String filePath) throws IOException {
        String jsonData = new String(Files.readAllBytes(Paths.get(filePath)));
        return jsonData.replaceAll("^\\p{C}", ""); // здесь помог chaGPT, потому что код почему то не хотел читать json,
        // и я убрал ненужные символы
    }

    private static Map<String, List<JSONObject>> groupFlightsByCarrier(JSONArray tickets) {
        Map<String, List<JSONObject>> flightsByCarrier = new HashMap<>();
        for (int i = 0; i < tickets.length(); i++) {
            JSONObject ticket = tickets.getJSONObject(i);
            String origin = ticket.getString("origin");
            String destination = ticket.getString("destination");
            if (origin.equals("VVO") && destination.equals("TLV")) {
                String carrier = ticket.getString("carrier");
                flightsByCarrier.computeIfAbsent(carrier, k -> new ArrayList<>()).add(ticket);
            }
        }
        return flightsByCarrier;
    }

    private static int calculateFlightDuration(JSONObject flight) {
        String departureTimeString = flight.getString("departure_time");
        String arrivalTimeString = flight.getString("arrival_time");

        if (departureTimeString.length() == 4) {
            departureTimeString = "0" + departureTimeString;
        }
        if (arrivalTimeString.length() == 4) {
            arrivalTimeString = "0" + arrivalTimeString;
        }

        LocalTime departureTime = LocalTime.parse(departureTimeString);
        LocalTime arrivalTime = LocalTime.parse(arrivalTimeString);

        return (int) (departureTime.until(arrivalTime, java.time.temporal.ChronoUnit.MINUTES));
    }

    private static int calculateMedian(List<Integer> prices) {
        List<Integer> sortedPrices = prices.stream().sorted().collect(Collectors.toList());
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return (sortedPrices.get(size / 2 - 1) + sortedPrices.get(size / 2)) / 2;
        } else {
            return sortedPrices.get(size / 2);
        }
    }
}
