package core;

public interface InputHandler {
    /**
     * Handle keyboard input
     *
     * @param key The key that was pressed
     * @return true if the input was handled, false otherwise
     */
    boolean handleInput(char key) throws InterruptedException;

    /**
     * Get the priority of this input handler.
     * Higher priority handlers get to process input first.
     */
}
