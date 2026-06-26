package com.techmart.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Singleton Session Bean — Application Configuration.
 *
 * Design Decisions:
 * - @Singleton + @Startup: Loads platform configuration once at startup.
 *   All EJBs read config from this bean rather than re-querying DB each time.
 * - @Lock(READ) for all getters — high-concurrency safe reads.
 * - @Lock(WRITE) for updates — exclusive write access.
 * - Configuration hot-reload via reload() without redeployment.
 * - Stores feature flags, limits, and operational parameters.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class ConfigurationBean {

    private static final Logger LOG = Logger.getLogger(ConfigurationBean.class.getName());

    private final Map<String, String> config = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        LOG.info("ConfigurationBean: Loading application configuration...");
        loadDefaults();
        LOG.info("ConfigurationBean: " + config.size() + " configuration entries loaded.");
    }

    private void loadDefaults() {
        // Platform limits
        config.put("max.cart.items",           "50");
        config.put("max.order.quantity",        "100");
        config.put("max.orders.per.day",        "10");
        config.put("session.timeout.minutes",   "30");

        // Shipping
        config.put("shipping.free.threshold",   "100.00");
        config.put("shipping.standard.cost",    "5.99");
        config.put("shipping.express.cost",     "15.99");

        // Inventory
        config.put("inventory.low.stock.threshold", "10");
        config.put("inventory.cache.refresh.minutes", "5");

        // Performance
        config.put("db.pool.min.connections",   "5");
        config.put("db.pool.max.connections",   "50");
        config.put("query.timeout.seconds",     "30");
        config.put("slow.query.threshold.ms",   "500");

        // Feature Flags
        config.put("feature.express.shipping",  "true");
        config.put("feature.guest.checkout",    "false");
        config.put("feature.wishlist",          "true");
        config.put("feature.reviews",           "true");
        config.put("feature.promotions",        "true");

        // Notification
        config.put("notifications.email.enabled", "true");
        config.put("notifications.sms.enabled",   "false");
        config.put("notifications.batch.size",     "100");

        // Application info
        config.put("app.name",    "TechMart Online");
        config.put("app.version", "1.0.0");
        config.put("app.environment", "production");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Lock(LockType.READ)
    public String get(String key) {
        return config.getOrDefault(key, "");
    }

    @Lock(LockType.READ)
    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    @Lock(LockType.READ)
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Lock(LockType.READ)
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(config.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Lock(LockType.READ)
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(config.getOrDefault(key, "false"));
    }

    @Lock(LockType.READ)
    public boolean isFeatureEnabled(String featureKey) {
        return getBoolean("feature." + featureKey);
    }

    @Lock(LockType.WRITE)
    public void set(String key, String value) {
        config.put(key, value);
        LOG.info("Config updated: " + key + "=" + value);
    }

    @Lock(LockType.WRITE)
    public void reload() {
        config.clear();
        loadDefaults();
        LOG.info("ConfigurationBean: Configuration reloaded.");
    }

    @Lock(LockType.READ)
    public Map<String, String> getAllConfig() {
        return new ConcurrentHashMap<>(config);
    }

    @Lock(LockType.READ)
    public String getAppName()    { return get("app.name"); }

    @Lock(LockType.READ)
    public String getAppVersion() { return get("app.version"); }

    @Lock(LockType.READ)
    public int getMaxCartItems()  { return getInt("max.cart.items", 50); }

    @Lock(LockType.READ)
    public int getMaxOrderQty()   { return getInt("max.order.quantity", 100); }

    @Lock(LockType.READ)
    public int getLowStockThreshold() { return getInt("inventory.low.stock.threshold", 10); }
}
