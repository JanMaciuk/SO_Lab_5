import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    protected static int numberOfProcessors = 50;
    protected static int numberOfTasks = 100; // na procesor
    protected static int loggingFrequency = 5; // Co ile jednostek czasu zapisuję obciążenie procesorów.
    protected static ArrayList<Processor> allProcessors = new ArrayList<>();
    protected static int requestsCount;
    protected static int transfersCount;

    public static void main(String[] args) {
        allProcessors = Generator.generateProcessors(numberOfProcessors, numberOfTasks);
        //run(1);
        allProcessors.forEach(Processor::reset);

    }

    public static void run(int strategyNumber) { // numer strategii
        requestsCount = 0;
        transfersCount = 0;
        int timeToLog = loggingFrequency;

        while(!allTasksDone()) {
            //Na każdym procesorze jest szansa na wygenerowanie nowego zadania
            for (Processor processor : allProcessors) {
                if (ThreadLocalRandom.current().nextBoolean() && ThreadLocalRandom.current().nextBoolean() && ThreadLocalRandom.current().nextBoolean()) {
                    // Procesor aktywuje zadanie (jeżeli zostały jeszcze jakieś zadania oczekujące)
                    if (!processor.activateTask()) continue;
                    // Jeżeli nie udało się aktywować zadania bo lista jest pusta to nie wykonujemy strategii.
                    // Wykonuję odpowiednie akcje zależnie od tego która strategia jest implementowana.
                    switch (strategyNumber) {
                        case 1 -> strategy1(processor);
                        case 2,3 -> strategy2(processor); // Strategia 2 i 3 mają takie samo działanie podczas aktywacji zadania
                    }
                }
            }
            // Upływa jedna jednostka czasu, każdy procesor wykonuje swoje zadania
            allProcessors.forEach(Processor::doActiveTasks);

            // Zapisuje obecne obciążenie procesorów raz na ileś jednostek czasu
            if (timeToLog == 0) {
                allProcessors.forEach(Processor::logLoad);
                timeToLog = loggingFrequency;
            } else {
                timeToLog--;
            }

            // Dobry samarytanin ze strategii 3
            if (strategyNumber == 3) { strategy3(); }

        }

        // Koniec symulacji, zwracam wyniki:
        int averageLoad = 0;
        for (Processor processor : allProcessors) { averageLoad += processor.getAverageLoad(); }
        averageLoad /= allProcessors.size();
        System.out.println("Średnie obciążenie wszystkich procesorów: " + averageLoad + "%");

        //Odchylenie standardowe
        double standardDeviation = 0.0;
        for (Processor process : allProcessors) {
            standardDeviation += Math.pow(process.getAverageLoad() - averageLoad, 2);
        }
        standardDeviation = Math.sqrt(standardDeviation / numberOfProcessors);
        System.out.println("Odchylenie standardowe: " + standardDeviation);


        System.out.println("Liczba zapytań do innych procesorów: " + requestsCount);
        System.out.println("Liczba przeniesień zadań pomiędzy procesorami: " + transfersCount);

        System.out.println("Obciążenie każdego procesora:");
        for (int i = 0; i < allProcessors.size(); i++) {
            System.out.println("Procesor " + i+1 + ": " + allProcessors.get(i).getAverageLoad() + "%");
        }


    }
    private static boolean allTasksDone() {
        for (Processor processor : allProcessors) { // Optymalny sposób bo najczęściej zwróci false po pierwszym porównaniu.
            if (!processor.waitingTasks.isEmpty() || !processor.runningTasks.isEmpty()) {
                return false;
            }
        }
        return true;
    }


    //TODO: implementacja strategii
    private static void strategy1(Processor currentProcessor) {}
    private static void strategy2(Processor currentProcessor) {}
    private static void strategy3() {
        //Losujemy procesy, jeżeli jakiś ma obciążenie poniżej minimalnego progu to odpytuje inne procesory.
    }


}