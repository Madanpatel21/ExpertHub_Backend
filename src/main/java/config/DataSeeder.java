package config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import Repository.roleRepository;
import entity.roles;

@Component
public class DataSeeder implements CommandLineRunner {

    private final roleRepository roleRepo;

    public DataSeeder(roleRepository roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound("USER");
        createRoleIfNotFound("CLIENT");
        createRoleIfNotFound("LAWYER");
    }

    private void createRoleIfNotFound(String name) {
        if (roleRepo.findByName(name).isEmpty()) {
            roles role = new roles();
            role.setName(name);
            roleRepo.save(role);
            System.out.println("Seeded Role: " + name);
        }
    }
}
