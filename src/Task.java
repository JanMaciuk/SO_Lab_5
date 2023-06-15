public class Task {
    protected int load;
    protected int timeLeft;
    protected int originalTime;
    public Task(int load, int timeLeft) {
        this.load = load;
        this.timeLeft = timeLeft;
        this.originalTime = timeLeft;
    }
    public void reset() {
        this.timeLeft = this.originalTime;
    }
}
