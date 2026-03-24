package com.camerarental.backend.config;

import com.camerarental.backend.model.entity.*;
import com.camerarental.backend.model.entity.enums.*;
import com.camerarental.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("local-pg")
@Order(2)
@RequiredArgsConstructor
public class LocalPgDataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CameraRepository cameraRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PhysicalUnitRepository physicalUnitRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        List<Camera> cameras = seedCameras();
        seedInventoryAndUnits(cameras);
        seedBusinessHours();
        log.info("Local-pg seed data loaded successfully");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already present – skipping");
            return;
        }

        Role customerRole = roleRepository.findByRoleName(AppRole.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER missing"));
        Role vendorRole = roleRepository.findByRoleName(AppRole.ROLE_VENDOR)
                .orElseThrow(() -> new IllegalStateException("ROLE_VENDOR missing"));
        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing"));

        User vendor = new User("vendor", "vendor@camerarental.dev", passwordEncoder.encode("password123"));
        vendor.setRoles(Set.of(vendorRole));

        User customer = new User("customer", "customer@camerarental.dev", passwordEncoder.encode("password123"));
        customer.setRoles(Set.of(customerRole));

        User admin = new User("admin", "admin@camerarental.dev", passwordEncoder.encode("password123"));
        admin.setRoles(Set.of(adminRole));

        userRepository.saveAll(List.of(vendor, customer, admin));
        log.info("Seeded 3 users: vendor / customer / admin");
    }

    private List<Camera> seedCameras() {
        if (cameraRepository.count() > 0) {
            log.info("Cameras already present – skipping");
            return cameraRepository.findAll();
        }

        Instant now = Instant.now();
        UUID seedUser = new UUID(0, 0);

        Camera sony = buildCamera("Sony", "FX6", CameraCategory.CINEMA, SensorFormat.FULL_FRAME,
                "Sony E", true, false, "4K UHD", 409600, 120, 240,
                "Full-frame cinema camera with outstanding low-light performance and 4K 120fps.",
                seedUser, now);

        Camera canonR5 = buildCamera("Canon", "EOS R5", CameraCategory.MIRRORLESS, SensorFormat.FULL_FRAME,
                "Canon RF", true, true, "45 MP", 51200, 60, 120,
                "Hybrid mirrorless with 8K RAW video and blazing autofocus.",
                seedUser, now);

        Camera bmpcc6k = buildCamera("Blackmagic", "Pocket Cinema Camera 6K G2", CameraCategory.CINEMA,
                SensorFormat.SUPER_35, "Canon EF", true, false, "6K", 25600, 60, 120,
                "Affordable cinema camera with Blackmagic RAW and 13 stops of dynamic range.",
                seedUser, now);

        Camera fujiXT5 = buildCamera("Fujifilm", "X-T5", CameraCategory.MIRRORLESS, SensorFormat.APS_C,
                "Fujifilm X", true, true, "40.2 MP", 51200, 60, 60,
                "Compact APS-C mirrorless with legendary Fujifilm colour science.",
                seedUser, now);

        Camera goPro12 = buildCamera("GoPro", "HERO12 Black", CameraCategory.ACTION_CAMERA, SensorFormat.APS_C,
                null, true, true, "27 MP", 6400, 120, 240,
                "Waterproof action camera with HyperSmooth stabilisation and 5.3K video.",
                seedUser, now);

        List<Camera> saved = cameraRepository.saveAll(List.of(sony, canonR5, bmpcc6k, fujiXT5, goPro12));
        log.info("Seeded 5 cameras");
        return saved;
    }

    private void seedInventoryAndUnits(List<Camera> cameras) {
        if (inventoryItemRepository.count() > 0) {
            log.info("Inventory already present – skipping");
            return;
        }

        Instant now = Instant.now();
        UUID seedUser = new UUID(0, 0);

        record Pricing(BigDecimal daily, BigDecimal replacement) {}

        List<Pricing> prices = List.of(
                new Pricing(new BigDecimal("350.00"), new BigDecimal("5998.00")),
                new Pricing(new BigDecimal("175.00"), new BigDecimal("3899.00")),
                new Pricing(new BigDecimal("125.00"), new BigDecimal("2495.00")),
                new Pricing(new BigDecimal("85.00"),  new BigDecimal("1699.00")),
                new Pricing(new BigDecimal("35.00"),  new BigDecimal("399.00"))
        );

        int[][] unitCounts = { {2}, {3}, {2}, {4}, {3} };

        UnitCondition[] conditions = { UnitCondition.NEW, UnitCondition.EXCELLENT, UnitCondition.GOOD, UnitCondition.FAIR };
        int unitSeq = 1;

        for (int i = 0; i < cameras.size(); i++) {
            Camera cam = cameras.get(i);
            Pricing p = prices.get(i);

            InventoryItem item = new InventoryItem();
            item.setCamera(cam);
            item.setDailyRentalPrice(p.daily);
            item.setReplacementValue(p.replacement);
            item.setCreatedBy(seedUser);
            item.setCreatedAt(now);
            item.setUpdatedAt(now);

            InventoryItem savedItem = inventoryItemRepository.save(item);

            int count = unitCounts[i][0];
            for (int u = 0; u < count; u++) {
                PhysicalUnit unit = new PhysicalUnit();
                unit.setInventoryItem(savedItem);
                unit.setSerialNumber(cam.getBrand().toUpperCase().substring(0, 3) + "-" + String.format("%04d", unitSeq++));
                unit.setCondition(conditions[u % conditions.length]);
                unit.setStatus(UnitStatus.AVAILABLE);
                unit.setAcquiredDate(LocalDate.of(2025, 1 + (u % 12), 15));
                unit.setCreatedBy(seedUser);
                unit.setCreatedAt(now);
                unit.setUpdatedAt(now);
                physicalUnitRepository.save(unit);
            }
        }

        log.info("Seeded {} inventory items with physical units", cameras.size());
    }

    private Camera buildCamera(String brand, String model, CameraCategory category,
                                SensorFormat sensor, String lensMount,
                                boolean video, boolean photo, String resolution,
                                int maxIso, int fps4k, int fps1080,
                                String description, UUID createdBy, Instant now) {
        Camera c = new Camera();
        c.setBrand(brand);
        c.setModelName(model);
        c.setCategory(category);
        c.setSensorFormat(sensor);
        c.setLensMount(lensMount);
        c.setActive(true);
        c.setVideoCapable(video);
        c.setPhotoCapable(photo);
        c.setResolution(resolution);
        c.setMaxIso(maxIso);
        c.setMaxFrameRate4k(fps4k);
        c.setMaxFrameRate1080p(fps1080);
        c.setDescription(description);
        c.setCreatedBy(createdBy);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return c;
    }

    private void seedBusinessHours() {
        if (businessHoursRepository.count() > 0) {
            log.info("Business hours already present – skipping");
            return;
        }

        Instant now = Instant.now();
        UUID seedUser = new UUID(0, 0);
        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(17, 0);

        List<BusinessHours> week = List.of(
                buildDay(DayOfWeek.MONDAY, open, close, false, seedUser, now),
                buildDay(DayOfWeek.TUESDAY, open, close, false, seedUser, now),
                buildDay(DayOfWeek.WEDNESDAY, open, close, false, seedUser, now),
                buildDay(DayOfWeek.THURSDAY, open, close, false, seedUser, now),
                buildDay(DayOfWeek.FRIDAY, open, LocalTime.of(15, 0), false, seedUser, now),
                buildDay(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0), false, seedUser, now),
                buildDay(DayOfWeek.SUNDAY, null, null, true, seedUser, now)
        );

        businessHoursRepository.saveAll(week);
        log.info("Seeded 7 business-hours entries");
    }

    private BusinessHours buildDay(DayOfWeek day, LocalTime open, LocalTime close,
                                   boolean closed, UUID createdBy, Instant now) {
        BusinessHours bh = new BusinessHours();
        bh.setDayOfWeek(day);
        bh.setOpenTime(open);
        bh.setCloseTime(close);
        bh.setClosed(closed);
        bh.setCreatedBy(createdBy);
        bh.setCreatedAt(now);
        bh.setUpdatedAt(now);
        return bh;
    }
}
