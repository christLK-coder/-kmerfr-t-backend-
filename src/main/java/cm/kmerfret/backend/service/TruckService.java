package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.CreateTruckRequest;
import cm.kmerfret.backend.dto.response.TruckResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.Truck;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.TruckRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TruckService {

    private final TruckRepository truckRepository;
    private final UserRepository userRepository;

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public TruckResponse createTruck(CreateTruckRequest req) {
        User driver = currentUser();
        Truck truck = Truck.builder()
                .driver(driver)
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .capacityTons(req.getCapacityTons())
                .truckType(Truck.TruckType.valueOf(req.getTruckType()))
                .insuranceExpiry(req.getInsuranceExpiry())
                .build();
        return TruckResponse.from(truckRepository.save(truck));
    }

    @Transactional(readOnly = true)
    public List<TruckResponse> getMyTrucks() {
        User driver = currentUser();
        return truckRepository.findByDriverId(driver.getId())
                .stream().map(TruckResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public TruckResponse updateTruck(UUID id, CreateTruckRequest req) {
        User driver = currentUser();
        Truck truck = truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camion introuvable : " + id));
        if (!truck.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("Ce camion ne vous appartient pas");
        }
        truck.setPlateNumber(req.getPlateNumber());
        truck.setBrand(req.getBrand());
        truck.setModel(req.getModel());
        truck.setCapacityTons(req.getCapacityTons());
        truck.setTruckType(Truck.TruckType.valueOf(req.getTruckType()));
        truck.setInsuranceExpiry(req.getInsuranceExpiry());
        return TruckResponse.from(truckRepository.save(truck));
    }
}
