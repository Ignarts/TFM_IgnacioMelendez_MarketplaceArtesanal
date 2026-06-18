package com.marketplace.shop;

import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.shop.dto.CreateShopRequest;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserService userService;

    public ShopService(ShopRepository shopRepository, UserService userService) {
        this.shopRepository = shopRepository;
        this.userService = userService;
    }

    /**
     * Opens a shop for the user and grants the SELLER role. Authorities are loaded from the
     * database on every request, so the new role takes effect on the next call (no re-login).
     */
    @Transactional
    public Shop openShop(User owner, CreateShopRequest request) {
        if (shopRepository.existsByOwnerId(owner.getId())) {
            throw new ConflictException("User already owns a shop");
        }

        Shop shop = shopRepository.save(new Shop(owner.getId(), request.name(), request.description()));

        owner.getRoles().add(Role.SELLER);
        userService.save(owner);

        return shop;
    }

    public Shop getByOwner(User owner) {
        return shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new NotFoundException("User has no shop"));
    }
}
