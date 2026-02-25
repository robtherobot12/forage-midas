package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Balance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceEndpoint {

    private final DatabaseConduit conduit;

    public BalanceEndpoint(DatabaseConduit conduit) {
        this.conduit = conduit;
    }

    @GetMapping("/balance")
    public Balance balance(@RequestParam long userId) {
        return conduit.getBalance(userId);
    }
}
