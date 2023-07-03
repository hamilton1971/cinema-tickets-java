package uk.gov.dwp.uc.pairtest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImplTest {

    private TicketService ticketService;

    @Mock
    TicketPaymentService ticketPaymentService;

    @Mock
    SeatReservationService seatReservationService;

    @Rule
    public ExpectedException invalidPurchaseException = ExpectedException.none();

    @Before
    public void initialise() {
        ticketPaymentService = Mockito.mock(TicketPaymentService.class);
        seatReservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }


    @Test
    public void rejectNullAccount() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_ACCOUNT_ID);

        ticketService.purchaseTickets(null);
    }
   
    @Test
    public void rejectInvalidAccount() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_ACCOUNT_ID);

        var adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(Long.valueOf(0L), adultTicketRequest);
    }

    @Test
    public void rejectNoTicketRequests() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_NO_TICKET_REQUESTS);

        ticketService.purchaseTickets(Long.valueOf(1L));
    }

    @Test
    public void rejectMoreThanMaximumTickets() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_TOO_MANY_TICKETS);

        var ticketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, TicketServiceImpl.MAX_NUMBER_OF_TICKETS + 1);
        ticketService.purchaseTickets(Long.valueOf(1L), ticketRequest);      
    }
  
    @Test
    public void acceptMaximumTickets() {
        var ticketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, TicketServiceImpl.MAX_NUMBER_OF_TICKETS);
        ticketService.purchaseTickets(Long.valueOf(1L), ticketRequest);  
        verify(ticketPaymentService).makePayment(anyLong(), anyInt()); 
        verify(seatReservationService).reserveSeat(anyLong(), anyInt());   
    }

    @Test
    public void rejectNoAdultTickets() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_NO_ADULT_TICKETS);

        var childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        var infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets(Long.valueOf(1L), childTicketRequest, infantTicketRequest);      
    }

    @Test
    public void rejectInfantTicketsExceedAdultTickets() {
        invalidPurchaseException.expect(InvalidPurchaseException.class);
        invalidPurchaseException.expectMessage(TicketServiceImpl.ERROR_NOT_ENOUGH_ADULT_TICKETS);

       var infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        var adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(Long.valueOf(1L), infantTicketRequest, adultTicketRequest);  
    }

    @Test
    public void calculateCorrectTotalPayment() {
        var infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        var adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        var childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        ticketService.purchaseTickets(Long.valueOf(1L), infantTicketRequest, adultTicketRequest, childTicketRequest);

        verify(ticketPaymentService).makePayment(1L, 70);
    }

    @Test
    public void calculateCorrectTotalSeats() {
        var infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        var adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        var childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        ticketService.purchaseTickets(Long.valueOf(1L), infantTicketRequest, adultTicketRequest, childTicketRequest);

        verify(seatReservationService).reserveSeat(1L, 5);
    }
}
