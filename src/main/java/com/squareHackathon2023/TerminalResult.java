package com.squareHackathon2023;

public class TerminalResult {
    protected Data data;

    public String getFingerprint() {
        return data.obj.payment.card.fingerprint;
    }
    public String getPaymentId() {
        return data.id;
    }

    public class Data {
        private String id;
        protected Obj obj;

        public class Obj {
            protected Payment payment;

            public class Payment {
                protected CardDetails card;

                public class CardDetails {
                    private String fingerprint;
                }
            }
        }
    }
}
