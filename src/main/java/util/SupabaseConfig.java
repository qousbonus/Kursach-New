package util;

public class SupabaseConfig {

    private static SupabaseConfig instance;

    private SupabaseConfig() {
    }

    public static SupabaseConfig getInstance() {
        if (instance == null) instance = new SupabaseConfig();
        return instance;
    }

    public String getUrl() {
        return ApiConfig.SUPABASE_URL;
    }

    public String getAnonKey() {
        return ApiConfig.SUPABASE_ANON_KEY;
    }

    public String getBucket() {
        return ApiConfig.STORAGE_BUCKET;
    }
}
