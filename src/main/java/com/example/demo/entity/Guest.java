package com.example.demo.entity;

import com.example.demo.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Guest {
//everybody including the managers are guests in the system. A user is at least a Guest and can have other
//roles. So it is not important that a guest is associated with a booking.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // A guest does not necessarily corrspond to the same user.

    //A single user could be booking hotels for his friends and family. Those frieds and family would be the guests.

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer age;
}

