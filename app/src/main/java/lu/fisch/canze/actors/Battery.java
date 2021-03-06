/*
    CanZE
    Take a closer look at your ZE car

    Copyright (C) 2015 - The CanZE Team
    http://canze.fisch.lu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.canze.actors;

/**
 * Battery
 */
public class Battery {


    /*
     * Meta code on usage
     *
     * Battery battery;
     * // initialize the values. No need to initialize dcPower
     * battery = new Battery();
     * battery.setTemperature (...);
     * battery.setStateOfCharge (...);
     * battery.setChargerPower (...);
     * for (int t=1; t<=60; t++) {
     *      draw (battery, t); // imaginary method that plots SOC, range, time
     *      battery.iterateCharging (60);
     * }
     *
     * Some rough parameters derived from this document: http://www.cse.anl.gov/us%2Dchina%2Dworkshop%2D2011/pdfs/batteries/LiFePO4%20battery%20performances%20testing%20for%20BMS.pdf
     *
     */

    private double temperature = 10.0;
    private double stateOfCharge = 11.0;                // watch it: in kWh!!!
    private double chargerPower = 11.0;                 // in kW
    private double capacity = 22.0;                     // in kWh
    private double maxDcPower = 0;                      // in kW. This excludes the max imposed by the external charger
    private double dcPower = 0;                         // in kW This includes the max imposed by the external charger

    private void predictMaxDcPower () {
        if (stateOfCharge >= capacity) {
            maxDcPower = 0.0;
        } else {
            double stateOfChargePercentage = stateOfCharge * 100.0 / capacity;
            int intTemperature = (int )temperature;
            maxDcPower = 19.0 + (3.6 * intTemperature) - (0.026 * stateOfChargePercentage * intTemperature) - (0.34 * stateOfChargePercentage);
            if (maxDcPower > 40.0) {
                maxDcPower = 40.0;
            } else if (maxDcPower < 2.0) {
                maxDcPower = 2.0;
            }
        }
    }

    private void predictDcPower() {
        predictMaxDcPower();
        double efficiency = 0.80 + maxDcPower * 0.00375;
        double requestedAcPower = maxDcPower / efficiency;
        if (requestedAcPower > chargerPower) {
            efficiency = 0.80 + chargerPower * 0.00375;
            dcPower = chargerPower * efficiency;
        } else {
            dcPower = maxDcPower;
        }

    }

    /*
     * iteration is in effect numerical integration of the power function to the SOC, respecting temperature and energy efficiency effects
     */

    public void iterateCharging (int seconds) {
        predictDcPower ();
        setTemperature (temperature + (seconds * dcPower / 7200)); // assume one degree per 40 kW per 3 minutes (180 seconds)
        setStateOfCharge (stateOfCharge + (dcPower * 0.95) / 60); // 1kW adds 95% of 1kWh in 60 minutes
    }

    /*
     * Getters and setters
     */

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        capacity = temperature > 15.0 ? 22.0 : (temperature > 0 ? 19.8 + temperature * 2.2 /15.0 : (19.8 + temperature * 4.4 /15.0));
        setStateOfCharge (getStateOfCharge());
    }

    public double getStateOfCharge() {
        return stateOfCharge;
    }

    public void setStateOfCharge(double stateOfCharge) {
        this.stateOfCharge = stateOfCharge;
        if (this.stateOfCharge > this.capacity) this.stateOfCharge = this.capacity;
    }

    public double getChargerPower() {
        return chargerPower;
    }

    public void setChargerPower(double chargerPower) {
        this.chargerPower = chargerPower;
        if (this.chargerPower > 43.0) {
            this.chargerPower = 43.0;
        } else if (this.chargerPower < 1.84) {
            this.chargerPower = 1.84;
        }
    }
    public double getMaxDcPower() {
        return maxDcPower;
    }

    public double getDcPower() {
        predictDcPower ();
        return dcPower;
    }
}
