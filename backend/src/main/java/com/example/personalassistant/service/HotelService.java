package com.example.personalassistant.service;

import com.example.personalassistant.dto.*;
import com.example.personalassistant.enums.AccountEnum;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.personalassistant.entity.Hotel;
import com.example.personalassistant.security.JwtUtil;
import com.example.personalassistant.entity.HotelLogin;
import com.example.personalassistant.repository.HotelRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.personalassistant.repository.UserLoginRepository;
import com.example.personalassistant.repository.HotelLoginRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class HotelService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HotelLoginRepository hotelLoginRepository;

    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Response> registerHotel(HotelDto hotelDto){
        Response response = new Response();
        if (hotelLoginRepository.existsByEmail(hotelDto.getEmail())){ //made changes
            ErrorDetails errorDetails = new ErrorDetails(HttpStatus
                    .BAD_REQUEST,
                    "hotel already registered:");
            response.setError(errorDetails);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (hotelDto.getEmail() == null || !com.example.personalassistant.util.DomainValidator.isGenuineDomain(hotelDto.getEmail())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "Only genuine email domains (e.g., Gmail, Yahoo, Outlook, iCloud) are allowed!"
            );
            response.setError(error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Hotel hotel = new Hotel();
        hotel.setHotel(hotelDto.getHotel());
        hotel.setCity(hotelDto.getCity());
        hotel.setEmail(hotelDto.getEmail());
        hotel.setAddress(hotelDto.getAddress());
        hotel.setPrice(hotelDto.getPrice());
        hotel.setLocation(hotelDto.getLocation());
        hotel.setAvailableRooms(hotelDto.getRoomAvailable());
        hotel.setAccountEnum(AccountEnum.ACTIVE);
        Hotel savedHotel = hotelRepository.save(hotel);

        HotelLogin login = new HotelLogin();
        login.setEmail(hotelDto.getEmail());
        login.setPassword(passwordEncoder.encode(hotelDto.getPassword()));
        hotelLoginRepository.save(login);

        response.setData(savedHotel);
        return ResponseEntity.ok(response);
    }


    public ResponseEntity<Response> loginHotel(String email, String password) {
        Response response = new Response();

        // Step 1: Check if email exists
        HotelLogin loginOptional = hotelLoginRepository.findByEmail(email).orElse(null);
        if (loginOptional == null) {
                ErrorDetails error = new ErrorDetails(
                        HttpStatus.NOT_FOUND,
                        "Hotel does not exist"
                );
                response.setError(error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        // Step 2: Validate password
        if (!passwordEncoder.matches(password, loginOptional.getPassword())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "Incorrect password!"
            );
            response.setError(error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // Step 3: Generate JWT token
        String hotelToken = jwtUtil.generateToken(email,"Hotel");

        response.setData(hotelToken);
        return ResponseEntity.ok(response);
    }

    public Hotel updateProfile(String email, UpdateHotelProfile request) {

        Hotel hotel = hotelRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(" something went wrong"));

        hotel.setHotel(request.getHotel());
        hotel.setAddress(request.getHotel());
        hotel.setCity(request.getCity());
        hotel.setPrice(request.getPrice());
        return hotelRepository.save(hotel);
    }
//
//    public Hotel deleteHotel(String email, DeleteAccount deleteAccountReq ) {//recently add this ACTIVE INACTIVE ACCOUNT 21/02/2026
//        Response response = new Response();
//        Optional<HotelLogin> isAccountExist = hotelLoginRepository.findById(deleteAccountReq.getEmail());
//        if (!isAccountExist.isPresent()){
//        ErrorDetails errorDetails = new ErrorDetails(HttpStatus
//                .NOT_FOUND,
//                "Hotel does not exist with this id:");
//        response.setError(errorDetails);
//        return ResponseEntity.(response);
//        }
//    }


    public String extractEmailFromToken(String hotelToken) {
        hotelToken = hotelToken.replace("Bearer ", "");
        return jwtUtil.extractUsername(hotelToken);
    }

    public Hotel getHotelByEmail(String email) {
        Response response = new Response();
        return hotelRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Hotel not found"));
    }



}

