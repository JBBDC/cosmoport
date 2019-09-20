package com.space.controller;

import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.service.impl.ShipServiceImpl;
import net.kaczmarzyk.spring.data.jpa.domain.*;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class Controller {
    @Autowired
    private ShipServiceImpl shipService;

    @PostMapping(value = "/rest/ships/{id}",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Ship> updateShip(@RequestBody Map<String,String> dataParams, @PathVariable String id
    ) {
        if (checkId(id)) return new ResponseEntity(HttpStatus.BAD_REQUEST);

        Ship ship = null;
        try {
            ship = shipService.getShipById(Long.parseLong(id));
        } catch (NoSuchElementException e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        Calendar c = Calendar.getInstance();
        if(dataParams.containsKey("prodDate") && dataParams.get("prodDate") != null) {
            c.setTimeInMillis(Long.parseLong(dataParams.get("prodDate")));
            int prodYear = c.get(Calendar.YEAR);
            if (dataParams.containsKey("prodDate") && dataParams.get("prodDate") != null) {
                if(prodYear < 2800 || prodYear > 3019) {
                    return new ResponseEntity(HttpStatus.BAD_REQUEST);
                }
                ship.setProdDate(c.getTime());
            }
        }

        if(dataParams.containsKey("name") && dataParams.get("name").isEmpty()){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        else if(dataParams.containsKey("name") && dataParams.get("name").length()>0 && dataParams.get("name").length() < 50) {
            String name = dataParams.get("name");
            ship.setName(name);
        }

        if(dataParams.containsKey("planet") && dataParams.get("planet").length()>0 && dataParams.get("planet").length() < 50) {
            String planet = dataParams.get("planet");
            ship.setPlanet(planet);
        }
        if(dataParams.containsKey("shipType") && dataParams.get("shipType")!=null) {
            ShipType shipType = ShipType.valueOf(dataParams.get("shipType"));
            ship.setShipType(shipType);
        }
        if(dataParams.containsKey("speed") && dataParams.get("speed")!=null &&
                Double.parseDouble(dataParams.get("speed"))<=0.99 && Double.parseDouble(dataParams.get("speed"))>=0.01) {
            Double speed = Double.parseDouble(dataParams.get("speed"));
            ship.setSpeed(speed);
        }
        if(dataParams.containsKey("crewSize") && dataParams.get("crewSize")!=null) {
            if(Integer.parseInt(dataParams.get("crewSize")) > 9999 || Integer.parseInt(dataParams.get("crewSize"))<1) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }
            int crewSize = Integer.parseInt(dataParams.get("crewSize"));
            ship.setCrewSize(crewSize);
        }
        if(dataParams.containsKey("isUsed")){
                Boolean isUsed =  Boolean.valueOf((dataParams.get("isUsed").isEmpty())?false:Boolean.valueOf(dataParams.get("isUsed")));
                ship.setUsed(isUsed);
        }

        if(!dataParams.isEmpty()) {
            c.setTimeInMillis(ship.getProdDate().getTime());
            ship.setRating();
            shipService.addShip(ship);
        }
        return new ResponseEntity(ship,HttpStatus.OK);
    }


    @PostMapping(value = "/rest/ships",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Ship> createShip(@RequestBody Map<String,String> dataParams
    ){
        Calendar c = Calendar.getInstance();
        int prodYear = 0;
        String name = null;
        String planet = null;
        Long prodDate = null;
        ShipType shipType = null;
        Double speed = null;
        int crewSize = 0;
        Boolean isUsed = false;

        try {
            c.setTimeInMillis(Long.parseLong(dataParams.get("prodDate")));
            prodYear = c.get(Calendar.YEAR);
            name = dataParams.get("name");
            planet = dataParams.get("planet");
            prodDate = Long.parseLong(dataParams.get("prodDate"));
            shipType = ShipType.valueOf(dataParams.get("shipType"));
            speed = Double.parseDouble(dataParams.get("speed"));
            crewSize = Integer.parseInt(dataParams.get("crewSize"));

            if(dataParams.containsKey("isUsed")){
                isUsed =  Boolean.valueOf((dataParams.get("isUsed").isEmpty())?false:Boolean.valueOf(dataParams.get("isUsed")));
            }

            if(name.isEmpty() || planet.isEmpty() || name.length()>50 || planet.length()>50 ||
                    shipType == null || prodDate == null || speed == null || dataParams.get("crewSize") == null || prodDate<0 || prodYear<2800 ||
                    prodYear > 3019 || speed < 0.01 || speed > 0.99 || crewSize < 1 || crewSize > 9999) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

        } catch (NullPointerException | NumberFormatException e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        Ship ship = new Ship();
        ship.setName(name);
        ship.setPlanet(planet);
        ship.setShipType(shipType);
        ship.setProdDate(c.getTime());
        ship.setUsed(isUsed);
        ship.setSpeed(speed);
        ship.setCrewSize(crewSize);
        ship.setRating();

        shipService.addShip(ship);
        return new ResponseEntity(ship,HttpStatus.OK);
    }


    @DeleteMapping(value = "/rest/ships/{id}",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity deleteShip(@PathVariable Long id){
        if(id <= 0){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        Ship ship = new Ship();
        try {
            shipService.delete(id);
        } catch (EmptyResultDataAccessException e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Ship>(ship,HttpStatus.OK);
    }


    @GetMapping(value = "/rest/ships/count",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE}
            )
    public Integer getCount(@And(value = {
            @Spec(path = "name", spec = Like.class),
            @Spec(path = "planet", spec = Like.class),
            @Spec(path = "shipType", spec = Equal.class),
            @Spec(path = "isUsed", spec = Equal.class),
            @Spec(path = "speed", params = "minSpeed", spec = GreaterThanOrEqual.class),
            @Spec(path = "speed", params = "maxSpeed", spec = LessThanOrEqual.class),
            @Spec(path = "crewSize", params = "minCrewSize", spec = GreaterThanOrEqual.class),
            @Spec(path = "crewSize", params = "maxCrewSize", spec = LessThanOrEqual.class),
            @Spec(path = "rating", params = "minRating", spec = GreaterThanOrEqual.class),
            @Spec(path = "rating", params = "maxRating", spec = LessThanOrEqual.class),
            }) Specification<Ship> specs , @RequestParam(value = "after",required = false) Long after,
                                            @RequestParam(value = "before",required = false) Long before){
        List<Ship> list = shipService.getAll(specs);
        List<Ship> result = new ArrayList<>();
        FilterDate(after,before,list,result);
        return result.size();
    }

    @GetMapping(value = "/rest/ships/{id}",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Ship> getShip(@PathVariable String id){

        if (checkId(id)) return new ResponseEntity(HttpStatus.BAD_REQUEST);

        Ship ship = new Ship();
    try {
            ship =  shipService.getShipById(Long.parseLong(id));
    } catch (NoSuchElementException e) {
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }
        return new ResponseEntity<Ship>(ship,HttpStatus.OK);
    }



    ;

    @RequestMapping(value = "/rest/ships"
                ,method = RequestMethod.GET
                ,produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<Ship> getShips(@And(value = {
            @Spec(path = "name", spec = Like.class),
            @Spec(path = "planet", spec = Like.class),
            @Spec(path = "shipType", spec = Equal.class),
            @Spec(path = "isUsed", spec = Equal.class),
            @Spec(path = "speed", params = "minSpeed", spec = GreaterThanOrEqual.class),
            @Spec(path = "speed", params = "maxSpeed", spec = LessThanOrEqual.class),
            @Spec(path = "crewSize", params = "minCrewSize", spec = GreaterThanOrEqual.class),
            @Spec(path = "crewSize", params = "maxCrewSize", spec = LessThanOrEqual.class),
            @Spec(path = "rating", params = "minRating", spec = GreaterThanOrEqual.class),
            @Spec(path = "rating", params = "maxRating", spec = LessThanOrEqual.class),
    })Specification<Ship> specs,@RequestParam(value = "after",required = false) Long after,
                                @RequestParam(value = "before",required = false) Long before,
                                @RequestParam(value = "pageNumber",required = false,defaultValue = "0") Integer pageNumber,
                                @RequestParam(value = "pageSize",required = false,defaultValue = "3") Integer pageSize,
                                @RequestParam(value = "order",required = false,defaultValue = "ID") ShipOrder order){
        List<Ship> shipList = shipService.getAll(specs);
        List<Ship> result = new ArrayList<>();
        FilterDate(after, before, shipList, result);
        SortByOrder(order,result);
        return getShipsOnPage(pageNumber,pageSize,result);
    }

    public List<Ship> getShipsOnPage(Integer pageNumber, Integer pageSize, List<Ship> ships) {
        int skip = pageNumber * pageSize;
        List<Ship> result = new ArrayList<>();
        for (int i = skip; i < Math.min(skip + pageSize, ships.size()); i++) {
            result.add(ships.get(i));
        }
        return result;
    }

    private void SortByOrder(ShipOrder order, List<Ship> ships){
        if (order == ShipOrder.ID) {
            ships.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));
        } else if (order == ShipOrder.DATE) {
            ships.sort((o1, o2) -> {
                if (o1.getProdDate().getTime() > o2.getProdDate().getTime())
                return 1;
            else if (o1.getProdDate().getTime() == (o2.getProdDate().getTime()))
                return 0;
            else
                return -1;
            });
        } else if (order == ShipOrder.SPEED) {
            ships.sort((o1, o2) -> {
                if (o1.getSpeed() > o2.getSpeed())
                    return 1;
                else if (o1.getSpeed().equals(o2.getSpeed()))
                    return 0;
                else
                    return -1;
            });
        } else if (order == ShipOrder.RATING) {
            ships.sort((o1, o2) -> {
                if (o1.getRating() > o2.getRating() )
                    return 1;
                else if (o1.getRating().equals(o2.getRating() ))
                    return 0;
                else
                    return -1;
            });
        }
    }

    private void FilterDate(Long after, Long before, List<Ship> shipList, List<Ship> resultList) {
        for (Ship e : shipList) {
            if(after != null) {
                if(e.getProdDate().getTime()<after) continue;
            }
            if(before != null) {
                if(e.getProdDate().getTime()>before) continue;
            }
            resultList.add(e);
        }
    }
    private boolean checkId(@PathVariable String id) {
        try {
            if (Long.parseLong(id) <= 0) return true;
        } catch (NumberFormatException | NoSuchElementException e) {
            return true;
        }
        return false;
    }
}
