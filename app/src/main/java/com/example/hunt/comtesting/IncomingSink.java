package com.example.hunt.comtesting;

/**
 * Created by Hunt on 1/26/2015.
 */
public interface IncomingSink {
    // Called with the period between the last frequency
    // shift and the current one, and what type of transistion
    // it was (HIGH TO LOW or LOW TO HIGH).
    void handleNextBit(int transistionPeriod, boolean isHighToLow);
}
