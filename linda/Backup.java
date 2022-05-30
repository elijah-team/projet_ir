package linda;

public interface Backup {
    void save();

    void load();

    void setBackupPath(String filePath);
}