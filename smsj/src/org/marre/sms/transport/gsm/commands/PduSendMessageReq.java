/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is "SMS Library for the Java platform".
 *
 * The Initial Developer of the Original Code is Markus Eriksson.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.marre.sms.transport.gsm.commands;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.marre.sms.SmsException;
import org.marre.sms.transport.gsm.GsmComm;
import org.marre.sms.transport.gsm.GsmException;
import org.marre.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a PDU mode Send Message Set request (AT+CMGS).
 * 
 * @author Markus
 * @version $Id$
 */
public class PduSendMessageReq
{
    private static Logger log_ = LoggerFactory.getLogger(PduSendMessageReq.class);
    
    private byte[] smscPdu_;
    private byte[] smsPdu_;

    private String endPduWith_ = "\032";
    
    /**
     * Send message in PDU mode using default SMSC.
     * 
     * @param smsPdu pdu for the sms data.
     */
    public PduSendMessageReq(byte[] smsPdu) {
        this(new byte[] {0x00}, smsPdu);
    }
    
    /**
     * Send message in PDU mode.
     * 
     * @param smscPdu pdu for the SMSC address.
     * @param smsPdu pdu for the sms data.
     */
    public PduSendMessageReq(byte[] smscPdu, byte[] smsPdu) {
        smscPdu_ = smscPdu;
        smsPdu_ = smsPdu;
    }

    /**
     * Set this if the send command should add an extra '\r' after
     * the PDU.
     * 
     * @param endPduWithCr
     */
    public void setEndPduWith(String endPduWith) {
        try
        {
            endPduWith_ = new String(StringUtil.hexStringToBytes(endPduWith), "ISO-8859-1");
        }
        catch (UnsupportedEncodingException e)
        {
            log_.error("Failed to use iso-8859-1", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Sends the command and builds a response object.
     * 
     * @param comm
     * @return
     * @throws GsmException
     * @throws IOException
     */
    public PduSendMessageRsp send(GsmComm comm) throws GsmException, IOException 
    {
        // <length> must indicate the number of octets coded in the TP layer data unit to 
        // be given (i.e. SMSC address octets are excluded)
        log_.debug("Sending AT+CMGS command");
        comm.send("AT+CMGS=" + smsPdu_.length + "\r");
        log_.debug("Read response from AT+CMGS command. Expecting a single '> ' without crlf.");
        readContinue(comm);

        // Build cmgs string.
        String cmgsPduString = StringUtil.bytesToHexString(smscPdu_) + StringUtil.bytesToHexString(smsPdu_);
        
        log_.debug("Send hexcoded PDU.");
        comm.send(cmgsPduString + endPduWith_);
        
        return readResponse(comm);
    }
    
    private PduSendMessageRsp readResponse(GsmComm comm) throws GsmException, IOException 
    {
        String response = comm.readLine();
        if (response.startsWith("+CMGS"))
        {
            // TODO: Parse message reference
            return new PduSendMessageRsp(null);
        } 
        else if (response.startsWith("+CMS ERROR:"))
        {
            throw new GsmException("CMS ERROR", response);
        } 
        else
        {
            throw new GsmException("Unexpected response", response);
        }
    }
    
    private void readContinue(GsmComm comm)
        throws GsmException, IOException
    {
        // Eat "> " that is sent from the device
        String response = comm.readLine("> ");
        
        if (! response.equals("> ")) {
            throw new GsmException("Unexpected response", response);
        }
    }
}
