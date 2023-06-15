import java.util.ArrayList;

public class Processor {
    protected ArrayList<Task> waitingTasks;
    protected ArrayList<Task> runningTasks;
    protected ArrayList<Task> doneTasks;
    protected ArrayList<Integer> loadHistory = new ArrayList<>();

    public Processor(ArrayList<Task> tasks) {
        waitingTasks = tasks;
        runningTasks = new ArrayList<>();
        doneTasks = new ArrayList<>();
    }

    public int currentLoad() {
        int load = 0;
        for (Task task : runningTasks) { load += task.load; }
        return load;
    }

    public void doActiveTasks() {
        runningTasks.forEach(task -> task.timeLeft--);
        for (Task task: runningTasks) {
            if (task.timeLeft >=0) doneTasks.add(task);
        }
        runningTasks.removeAll(doneTasks);
    }
    public boolean activateTask() {
        if (waitingTasks.size() > 0) {
            runningTasks.add(waitingTasks.remove(0));
            return true;
        }
        else return false;
    }

    public Task getBiggestTask() {
        if (runningTasks.isEmpty()) return new Task(0,0); // Puste zadanie które nigdy się nie wykona, więc nie wpłynie na symulacje. (odpowiednik null-a)
        Task biggestTask = runningTasks.get(0);
        for (Task task : runningTasks) {
            if (task.load > biggestTask.load) {
                biggestTask = task;
            }
        }
        return biggestTask;
    }
    public void reset() {
        // Przywracam wszystkie zadania do oczekujących i resetuje ich czas wykonania.
        waitingTasks.addAll(runningTasks);
        runningTasks.clear();
        waitingTasks.addAll(doneTasks);
        doneTasks.clear();
        waitingTasks.forEach(Task::reset);
    }

    public void logLoad() {
        // Zapisuje obecne obciążenie procesora.
        loadHistory.add(currentLoad());
    }

    public int getAverageLoad() {
        int sum = 0;
        for (Integer load : loadHistory) { sum += load; }
        return sum / loadHistory.size();
    }
}
