package com.jf.PetApp.infrastructure.controller.pet;

import com.jf.PetApp.application.pet.usecase.ConfigurePetUseCase;
import com.jf.PetApp.application.pet.usecase.GetPetStatusUseCase;
import com.jf.PetApp.application.pet.usecase.GetMyPetUseCase;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.core.security.SecurityUtils;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final ConfigurePetUseCase configurePetUseCase;
    private final GetPetStatusUseCase getPetStatusUseCase;
    private final GetMyPetUseCase getMyPetUseCase;

    public PetController(ConfigurePetUseCase configurePetUseCase, GetPetStatusUseCase getPetStatusUseCase, GetMyPetUseCase getMyPetUseCase) {
        this.configurePetUseCase = configurePetUseCase;
        this.getPetStatusUseCase = getPetStatusUseCase;
        this.getMyPetUseCase = getMyPetUseCase;
    }

    @PostMapping("/configure")
    public ResponseEntity<Void> configurePet(@RequestBody ConfigurePetRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        try {
            PetSpecieEnum specie = PetSpecieEnum.valueOf(request.specie().toUpperCase());
            configurePetUseCase.execute(email, specie, request.name());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid specie");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<PetStatusResponseDTO> getStatus() {
        String email = SecurityUtils.getCurrentUserEmail();
        boolean hasPet = getPetStatusUseCase.execute(email);
        return ResponseEntity.ok(new PetStatusResponseDTO(hasPet));
    }

    @GetMapping("/my-pet")
    public ResponseEntity<PetDetailResponseDTO> getMyPet() {
        String email = SecurityUtils.getCurrentUserEmail();
        Optional<Pet> petOpt = getMyPetUseCase.execute(email);
        if (petOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Pet pet = petOpt.get();
        return ResponseEntity.ok(new PetDetailResponseDTO(pet.getSpecie().name(), pet.getName(), pet.getHealth()));
    }

    public record ConfigurePetRequestDTO(String specie, String name) {}
    public record PetStatusResponseDTO(boolean hasPet) {}
    public record PetDetailResponseDTO(String specie, String name, int health) {}
}
