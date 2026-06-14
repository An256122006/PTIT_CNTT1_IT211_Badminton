package org.example.project.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.project.exception.HttpNotFoundException;
import org.example.project.exception.ValidAlreadyExistsException;
import org.example.project.model.entity.Booking;
import org.example.project.model.entity.Court;
import org.example.project.model.entity.User;
import org.example.project.model.dto.BookingRequestDto;
import org.example.project.repository.BookingRepository;
import org.example.project.repository.CourtRepository;
import org.example.project.repository.UserRepository;
import org.example.project.service.BookingService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    @Override
    public Booking createBooking(BookingRequestDto bookingDto) {
        Court court = courtRepository.findById(bookingDto.getCourtId()).orElseThrow(()->new HttpNotFoundException("Court not found"));
        User user = userRepository.findById(bookingDto.getUserId()).orElseThrow(()->new HttpNotFoundException("User not found"));
        String [] time=bookingDto.getTimeSlot().split("-");
        LocalTime startTime=LocalTime.parse(time[0]);
        LocalTime endTime=LocalTime.parse(time[1]);
        List<Booking> bookings=bookingRepository.findByCourtIdAndBookingDate(court.getId(), bookingDto.getBookingDate());
        for (Booking booking : bookings) {

            String[] oldSlot = booking.getTimeSlot().split("-");

            LocalTime oldStart = LocalTime.parse(oldSlot[0]);
            LocalTime oldEnd = LocalTime.parse(oldSlot[1]);

            if (oldStart.isBefore(endTime)
                    && oldEnd.isAfter(startTime)) {

                throw new ValidAlreadyExistsException(
                        "timeSlot",
                        "Khung giờ đã có người đặt"
                );
            }
        }

        Booking booking = new Booking();
        booking.setBookingDate(bookingDto.getBookingDate());
        booking.setCourt(court);
        booking.setUser(user);
        booking.setTimeSlot(bookingDto.getTimeSlot());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setStatus("PENDING");
        long hours= Duration.between(startTime, endTime).toHours();
        booking.setTotalPrice(court.getPrice()*hours);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking approveBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new HttpNotFoundException("Booking not found"));

        booking.setStatus("CONFIRMED");

        return bookingRepository.save(booking);
    }

    @Override
    public Booking rejectBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new HttpNotFoundException("Booking not found"));

        booking.setStatus("CANCELLED");

        return bookingRepository.save(booking);
    }
}
