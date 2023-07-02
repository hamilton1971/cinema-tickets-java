package uk.gov.dwp.uc.pairtest;

import java.util.stream.Stream;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    final static int MAX_NUMBER_OF_TICKETS = 20;
    final static String ERROR_ACCOUNT_ID = "Account id must be defined and be greater than 0";
    final static String ERROR_NO_TICKET_REQUESTS = "At least one ticket request must be specified";
    final static String ERROR_NO_ADULT_TICKETS = "Tickets can not be purchased without an adult ticket";
    final static String ERROR_NOT_ENOUGH_ADULT_TICKETS = "There are not enough adults for infants to sit on";
    final static String ERROR_TOO_MANY_TICKETS = "Number of tickets to purchase exceeds " + MAX_NUMBER_OF_TICKETS;

    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;

    TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        if (accountId == null || accountId < 1) {
            throw new InvalidPurchaseException(ERROR_ACCOUNT_ID);
        }

        if (ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(ERROR_NO_TICKET_REQUESTS);
        }

        if (!isThereAnAdultTicket(ticketTypeRequests)) {
            throw new InvalidPurchaseException(ERROR_NO_ADULT_TICKETS);
        }

        if (!isThereEnoughAdultsForInfants(ticketTypeRequests)) {
            throw new InvalidPurchaseException(ERROR_NOT_ENOUGH_ADULT_TICKETS);
        }

        var totalTickets = calculateTotalTickets(ticketTypeRequests);
        if (totalTickets > MAX_NUMBER_OF_TICKETS) {
            throw new InvalidPurchaseException(ERROR_TOO_MANY_TICKETS);
        } 

        var totalPayment = calculateTotalPayment(ticketTypeRequests);
        ticketPaymentService.makePayment(accountId, totalPayment);
        var totalSeats = calculateTotalSeats(ticketTypeRequests);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    private int calculateTotalTickets(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests).mapToInt(r -> r.getNoOfTickets()).sum();
    }

    private boolean isThereAnAdultTicket(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests).anyMatch(r -> r.getTicketType() == TicketTypeRequest.Type.ADULT && r.getNoOfTickets() > 0);
    }

    /**
     * Establishes if there is enough adults to sit all the infants on their lap.
     * An infant can sit on one adult's lap.
     * @param ticketTypeRequests
     * @return 
     */
    private boolean isThereEnoughAdultsForInfants(TicketTypeRequest[] ticketTypeRequests) {
        var totalAdults = Stream.of(ticketTypeRequests)
                .filter(r -> r.getTicketType() == TicketTypeRequest.Type.ADULT)
                .mapToInt(r -> r.getNoOfTickets())
                .sum();
        var totalInfants = Stream.of(ticketTypeRequests)
                .filter(r -> r.getTicketType() == TicketTypeRequest.Type.INFANT)
                .mapToInt(r -> r.getNoOfTickets())
                .sum();
        return totalAdults >= totalInfants;
    }

    private int calculateTotalPayment(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests).mapToInt(r -> r.getTicketType().price * r.getNoOfTickets()).sum();
    }

    /**
     * Calculates total number of seats for all adults and children.
     * Infants sit on adults' lap and therefore are excluded.
     * @param ticketTypeRequests
     * @return
     */
    private int calculateTotalSeats(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests)
                    .filter(r -> r.getTicketType() == TicketTypeRequest.Type.ADULT || r.getTicketType() == TicketTypeRequest.Type.CHILD)
                    .mapToInt(r -> r.getNoOfTickets())
                    .sum();
    }
}
