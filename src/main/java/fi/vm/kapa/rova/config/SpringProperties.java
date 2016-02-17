package fi.vm.kapa.rova.config;

public interface SpringProperties {

    String ENGINE_URL = "${engine_url}";
    String ENGINE_API_KEY = "${engine_api_key}";
    String REQUEST_ALIVE_SECONDS = "${request_alive_seconds}";

    String VARE_URL = "${vare_url}";
    String VARE_API_KEY = "${vare_api_key}";
}
