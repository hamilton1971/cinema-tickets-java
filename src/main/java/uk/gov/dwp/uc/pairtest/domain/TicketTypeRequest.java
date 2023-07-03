package uk.gov.dwp.uc.pairtest.domain;

 /**
  * Holds the type of ticket and its price plus the number of tickets required.
  * Each instance should be immutable.
  */
public class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT(20), CHILD(10) , INFANT(0);

        public final int price;

        private Type(int price) {
            this.price = price;
        }
    }

}
