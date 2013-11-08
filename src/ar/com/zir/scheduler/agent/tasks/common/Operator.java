/*
    Copyright 2012 Juan Martín Runge
    
    jmrunge@gmail.com
    http://www.zirsi.com.ar
    
    This file is part of SchedulerAgent.

    SchedulerAgent is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SchedulerAgent is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SchedulerAgent.  If not, see <http://www.gnu.org/licenses/>.
*/
package ar.com.zir.scheduler.agent.tasks.common;

/**
 *
 * @author jmrunge
 */
public enum Operator {
    EQUAL,
    LESS_OR_EQUAL,
    LESS,
    GREATER_OR_EQUAL,
    GREATER;
    
    public static Operator getOperator(String operator) throws Exception {
        switch (operator.trim()) {
            case "less":
                return Operator.LESS;
            case "lessEqual":
                return Operator.LESS_OR_EQUAL;
            case "equal":
                return Operator.EQUAL;
            case "greater":
                return Operator.GREATER;
            case "greaterEqual":
                return Operator.GREATER_OR_EQUAL;
            default:
                throw new Exception("Invalid operator [" + operator + "]");
        }
    }
}
