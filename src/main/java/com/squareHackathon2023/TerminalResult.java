package com.squareHackathon2023;

public class TerminalResult {
    protected Data data;

    public String getFingerprint() {
        return data.obj.payment.card.fingerprint;
    }
    public String getPaymentId() {
        return data.id;
    }
    public String getDeviceId() {
        return data.obj.deviceCode.deviceId;
    }

    public class Data {
        private String id;
        protected Obj obj;

        public class Obj {
            protected Payment payment;
            protected DeviceCode deviceCode;

            public class Payment {
                protected CardDetails card;

                public class CardDetails {
                    private String fingerprint;
                }
            }
            
            public class DeviceCode {
                protected String deviceId;
            }

        }
    }
}
