package service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import Repository.userRepository;
import entity.user;

@Service
public  class UserInfoService implements  UserDetailsService {
	@Autowired
    private userRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up the user by phone number
        Optional<user> userDetail = repository.findByPhoneNumber(username);

        // Convert the 'user' entity into the 'UserInfoDetails' wrapper
        return userDetail.map(UserInfoDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));
    }
		
}
