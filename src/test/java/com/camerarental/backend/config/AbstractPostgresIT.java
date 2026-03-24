package com.camerarental.backend.config;

import com.camerarental.backend.model.entity.*;
import com.camerarental.backend.model.entity.enums.AppRole;
import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import com.camerarental.backend.model.entity.enums.UnitCondition;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import com.camerarental.backend.repository.*;
import com.camerarental.backend.security.services.UserDetailsImpl;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Abstract base class for integration tests using PostgreSQL via TestContainers.
 *
 * <p>Shared PostgreSQL container (singleton) for performance.
 * No @Transactional on tests - each HTTP request runs in its own transaction like production.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @PostgresIntegrationTest
 * class MyControllerIT extends AbstractPostgresIT {
 *
 *     @BeforeEach
 *     void setUp() {
 *         cleanDatabase();
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("camerarental_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        POSTGRES.start();
    }

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected CameraRepository cameraRepository;

    @Autowired
    protected InventoryItemRepository inventoryItemRepository;

    @Autowired
    protected PhysicalUnitRepository physicalUnitRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    // =========================================================================
    // Database Cleanup
    // =========================================================================

    /**
     * Cleans all test data from the database.
     * Tables are truncated in the correct order to respect foreign key constraints.
     */
    protected void cleanDatabase() {
        entityManager.clear();

        jdbcTemplate.execute("TRUNCATE TABLE " +
                "physical_unit, " +
                "inventory_item, " +
                "camera, " +
                "business_hours, " +
                "user_role, " +
                "users, " +
                "roles " +
                "RESTART IDENTITY CASCADE"
        );
    }

    protected void clearEntityCache() {
        entityManager.clear();
    }

    // =========================================================================
    // Transaction Helpers
    // =========================================================================

    protected void inTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    protected <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    protected void initializeCollection(Collection<?> collection) {
        Hibernate.initialize(collection);
    }

    // =========================================================================
    // Role Helpers
    // =========================================================================

    protected Role ensureRole(AppRole appRole) {
        return roleRepository.findByRoleName(appRole)
                .orElseGet(() -> roleRepository.save(new Role(appRole)));
    }

    protected Role ensureCustomerRole() {
        return ensureRole(AppRole.ROLE_CUSTOMER);
    }

    protected Role ensureVendorRole() {
        return ensureRole(AppRole.ROLE_VENDOR);
    }

    protected Role ensureAdminRole() {
        return ensureRole(AppRole.ROLE_ADMIN);
    }

    // =========================================================================
    // User Creation Helpers
    // =========================================================================

    protected User createUserWithRole(String username, String rawPassword, String email, AppRole role) {
        Role roleEntity = ensureRole(role);
        User user = new User(username, email, passwordEncoder.encode(rawPassword));
        user.getRoles().add(roleEntity);
        return userRepository.save(user);
    }

    protected User createCustomer() {
        return createUserWithRole("customer1", "Password123!", "customer1@example.com", AppRole.ROLE_CUSTOMER);
    }

    protected User createCustomer(String username) {
        return createUserWithRole(username, "Password123!", username + "@example.com", AppRole.ROLE_CUSTOMER);
    }

    protected User createAdmin() {
        return createUserWithRole("admin1", "Password123!", "admin1@example.com", AppRole.ROLE_ADMIN);
    }

    protected User createAdmin(String username) {
        return createUserWithRole(username, "Password123!", username + "@example.com", AppRole.ROLE_ADMIN);
    }

    // =========================================================================
    // Camera / Inventory Helpers
    // =========================================================================

    protected Camera createCamera(String brand, String model) {
        Camera c = new Camera();
        c.setBrand(brand);
        c.setModelName(model);
        c.setCategory(CameraCategory.MIRRORLESS);
        c.setSensorFormat(SensorFormat.FULL_FRAME);
        c.setActive(true);
        c.setVideoCapable(true);
        c.setPhotoCapable(true);
        c.setCreatedBy(new UUID(0, 0));
        c.setCreatedAt(java.time.Instant.now());
        c.setUpdatedAt(java.time.Instant.now());
        return cameraRepository.save(c);
    }

    protected InventoryItem createInventoryItem(Camera camera, String dailyPrice, String replacementValue) {
        InventoryItem item = new InventoryItem();
        item.setCamera(camera);
        item.setDailyRentalPrice(new BigDecimal(dailyPrice));
        item.setReplacementValue(new BigDecimal(replacementValue));
        item.setCreatedBy(new UUID(0, 0));
        item.setCreatedAt(java.time.Instant.now());
        item.setUpdatedAt(java.time.Instant.now());
        return inventoryItemRepository.save(item);
    }

    protected PhysicalUnit createPhysicalUnit(InventoryItem item, String serialNumber) {
        PhysicalUnit unit = new PhysicalUnit();
        unit.setInventoryItem(item);
        unit.setSerialNumber(serialNumber);
        unit.setCondition(UnitCondition.NEW);
        unit.setStatus(UnitStatus.AVAILABLE);
        unit.setAcquiredDate(LocalDate.of(2025, 1, 15));
        unit.setCreatedBy(new UUID(0, 0));
        unit.setCreatedAt(java.time.Instant.now());
        unit.setUpdatedAt(java.time.Instant.now());
        return physicalUnitRepository.save(unit);
    }

    // =========================================================================
    // Authentication Helpers
    // =========================================================================

    protected RequestPostProcessor authenticated(User user) {
        return user(UserDetailsImpl.build(user));
    }

    // =========================================================================
    // JSON Helpers
    // =========================================================================

    protected String toJson(Object obj) throws Exception {
        return jsonMapper.writeValueAsString(obj);
    }

    protected <T> T fromJson(String json, Class<T> type) throws Exception {
        return jsonMapper.readValue(json, type);
    }
}
