package com.warehouse.receiving.web;

import com.warehouse.receiving.domain.Inventory;
import com.warehouse.receiving.domain.Location;
import com.warehouse.receiving.mapper.InventoryMapper;
import com.warehouse.receiving.mapper.LocationMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only lookups for the UI: put-away locations and current stock.
 * Pass-through reads with no business rules; calling mappers directly is a
 * defensible shortcut here (adding a service layer would add nothing but a
 * file). The moment a rule appears, promote it to a service.
 */
@RestController
@RequestMapping("/api")
public class MasterDataController {

    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;

    public MasterDataController(LocationMapper locationMapper, InventoryMapper inventoryMapper) {
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @GetMapping("/locations")
    public List<Location> getLocations() {
        return locationMapper.findAll();
    }

    @GetMapping("/inventory")
    public List<Inventory> getInventory() {
        return inventoryMapper.findAll();
    }
}
