package com.workforceos.authentication;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
class UserAccountLookupService {

    private final UserAccountRepository userAccountRepository;

    UserAccountLookupService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Cacheable(value = "userAccount", key = "#username")
    UserAccount findUser(String username) {
        return userAccountRepository.findById(username).map(UserAccountEntity::toRecord).orElse(null);
    }
}
