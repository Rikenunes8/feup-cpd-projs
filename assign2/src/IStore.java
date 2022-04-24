public interface IStore {
    String put(String key, String value);
    String get(String key);
    boolean delete(String key);
}
