package org.clockin.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.clockin.domain.Authority;
import org.clockin.domain.User;
import org.clockin.repository.AuthorityRepository;
import org.clockin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Service;

@Service
public class SocialService {
    private final Logger log = LoggerFactory.getLogger(SocialService.class);

    @Inject
    private UsersConnectionRepository usersConnectionRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MailService mailService;

    public void deleteUserSocialConnection(String login) {
        ConnectionRepository connectionRepository = usersConnectionRepository
            .createConnectionRepository(login);
        connectionRepository.findAllConnections().keySet().stream()
            .forEach(providerId -> {
                connectionRepository.removeConnections(providerId);
                log.debug("Delete user social connection providerId: {}",
                    providerId);
            });
    }

    public void createSocialUser(Connection<?> connection, String langKey) {
        if (connection == null) {
            log.error("Cannot create social user because connection is null");
            throw new IllegalArgumentException("Connection cannot be null");
        }
        UserProfile userProfile = connection.fetchUserProfile();
        String providerId = connection.getKey().getProviderId();
        User user = createUserIfNotExist(userProfile, langKey, providerId);
        createSocialConnection(user.getLogin(), connection);
    }

    private User createUserIfNotExist(UserProfile userProfile, String langKey,
        String providerId) {
        String email = userProfile.getEmail();
        String userName = userProfile.getUsername();
        if (StringUtils.isBlank(email) && StringUtils.isBlank(userName)) {
            log.error(
                "Cannot create social user because email and login are null");
            throw new IllegalArgumentException(
                "Email and login cannot be null");
        }
        if (StringUtils.isBlank(email)
            && userRepository.findOneByLogin(userName).isPresent()) {
            log.error(
                "Cannot create social user because email is null and login already exist, login -> {}",
                userName);
            throw new IllegalArgumentException(
                "Email cannot be null with an existing login");
        }
        Optional<User> user = userRepository.findOneByEmail(email);
        if (user.isPresent()) {
            log.info(
                "User already exist associate the connection to this account");
            return user.get();
        }
        if (!email.endsWith("@liferay.com")) {
            log.error(
                "Only Liferay employees can create an account, login -> {}",
                userName);
            throw new IllegalArgumentException(
                "Only Liferay employees can create an account");
        }

        String login = userProfile.getEmail();
        String encryptedPassword = passwordEncoder
            .encode(RandomStringUtils.random(10));
        Set<Authority> authorities = new HashSet<>(1);
        authorities.add(authorityRepository.findOne("ROLE_USER"));

        User newUser = new User();
        newUser.setLogin(login);
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userProfile.getFirstName());
        newUser.setLastName(userProfile.getLastName());
        newUser.setEmail(email);
        newUser.setActivated(true);
        newUser.setAuthorities(authorities);
        newUser.setLangKey(langKey);

        return userRepository.save(newUser);
    }

    private void createSocialConnection(String login,
        Connection<?> connection) {
        ConnectionRepository connectionRepository = usersConnectionRepository
            .createConnectionRepository(login);
        connectionRepository.addConnection(connection);
    }
}
