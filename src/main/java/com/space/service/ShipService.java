package com.space.service;

import com.space.model.Ship;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.util.List;

@Service
public interface ShipService {
    Ship getShipById(Long id);
    void delete(Long id);
    List<Ship> getAll(Specification<Ship> specification);
    Ship addShip(Ship ship);
    Long count(Specification<Ship> specification);
}
