package virtualclientserver.actions;


import static virtualclientserver.actions.ActionType.INITIALISE;

public class InitialiseAction extends Action {

    private int width;
    private int height;
    private boolean spectating;

    public InitialiseAction(int width, int heigth, boolean spectating) {
        super(INITIALISE);
        this.width = width;
        this.height = heigth;
        this.spectating = spectating;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isSpectating() {
        return spectating;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }
}
