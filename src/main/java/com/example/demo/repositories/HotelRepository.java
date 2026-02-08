package com.example.demo.repositories;

import com.example.demo.entity.Hotel;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {


    List<Hotel> findByOwner(User user);
}
