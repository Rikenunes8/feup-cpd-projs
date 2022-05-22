public interface IService {
    void put(String key, String value);
    String get(String key);
    void delete(String key);
}
