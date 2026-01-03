package pt.ipleiria.common;

// DTO para um evento de tecla (apenas tempos e ordem)
public class KeystrokeEvent {
    public int index;     // Ordem da tecla (0, 1, 2...)
    public int seekTime;  // Tempo até atingir a tecla
    public int pressTime; // Tempo a pressionar a tecla

    public KeystrokeEvent(int index, int seekTime, int pressTime) {
        this.index = index;
        this.seekTime = seekTime;
        this.pressTime = pressTime;
    }
}
