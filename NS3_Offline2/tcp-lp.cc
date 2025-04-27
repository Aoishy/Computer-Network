/*
 * Copyright (c) 2016 NITK Surathkal
 *
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * Authors: Charitha Sangaraju <charitha29193@gmail.com>
 *          Nandita G <gm.nandita@gmail.com>
 *          Mohit P. Tahiliani <tahiliani@nitk.edu.in>
 *
 */

#include "tcp-lp.h"

#include "ns3/log.h"
#include "ns3/simulator.h"

namespace ns3
{

NS_LOG_COMPONENT_DEFINE("TcpLp");
NS_OBJECT_ENSURE_REGISTERED(TcpLp);

TypeId
TcpLp::GetTypeId()
{
    static TypeId tid = TypeId("ns3::TcpLp")
                            .SetParent<TcpNewReno>()
                            .AddConstructor<TcpLp>()
                            .SetGroupName("Internet");
    return tid;
}

TcpLp::TcpLp()
    : TcpNewReno(),
      m_flag(0),
      m_sOwd(0),
      m_owdMin(0xffffffff),
      m_owdMax(0),
      m_owdMaxRsv(0),
      m_lastDrop(Time(0)),
      m_inference(Time(0)),
      //////////////////
       m_instabilityThreshold(50)
      
{
    NS_LOG_FUNCTION(this);
}

TcpLp::TcpLp(const TcpLp& sock)
    : TcpNewReno(sock),
      m_flag(sock.m_flag),
      m_sOwd(sock.m_sOwd),
      m_owdMin(sock.m_owdMin),
      m_owdMax(sock.m_owdMax),
      m_owdMaxRsv(sock.m_owdMaxRsv),
      m_lastDrop(sock.m_lastDrop),
      m_inference(sock.m_inference),
      ///////////////////////
      m_instabilityThreshold(sock.m_instabilityThreshold)
{
    NS_LOG_FUNCTION(this);
}

TcpLp::~TcpLp()
{
    NS_LOG_FUNCTION(this);
}

Ptr<TcpCongestionOps>
TcpLp::Fork()
{
    return CopyObject<TcpLp>(this);
}

void
TcpLp::CongestionAvoidance(Ptr<TcpSocketState> tcb, uint32_t segmentsAcked)
{
    NS_LOG_FUNCTION(this << tcb << segmentsAcked);

    if (!(m_flag & LP_WITHIN_INF))
    {
        TcpNewReno::CongestionAvoidance(tcb, segmentsAcked);
    }
}

uint32_t
TcpLp::OwdCalculator(Ptr<TcpSocketState> tcb)
{
    NS_LOG_FUNCTION(this << tcb);

    int64_t owd = 0;

    owd = tcb->m_rcvTimestampValue - tcb->m_rcvTimestampEchoReply;

    if (owd < 0)
    {
        owd = -owd;
    }
    if (owd > 0)
    {
        m_flag |= LP_VALID_OWD;
    }
    else
    {
        m_flag &= ~LP_VALID_OWD;
    }
    return owd;
}

void
TcpLp::RttSample(Ptr<TcpSocketState> tcb)
{
    NS_LOG_FUNCTION(this << tcb);

    uint32_t mowd = OwdCalculator(tcb);

    if (!(m_flag & LP_VALID_OWD))
    {
        return;
    }

    /* record the next minimum owd */
    if (mowd < m_owdMin)
    {
        m_owdMin = mowd;
    }

    if (mowd > m_owdMax)
    {
        if (mowd > m_owdMaxRsv)
        {
            if (m_owdMaxRsv == 0)
            {
                m_owdMax = mowd;
            }
            else
            {
                m_owdMax = m_owdMaxRsv;
            }
            m_owdMaxRsv = mowd;
        }
        else
        {
            m_owdMax = mowd;
        }
    }

    /* Calculation for Smoothed Owd */
//////////////////////////track the long-term network delay trend
     int64_t owdDifference = mowd - m_sOwd;///////////////////////////
    if (m_sOwd != 0)
    {
        mowd -= m_sOwd >> 3;
        m_sOwd += mowd; /* owd = 7/8 owd + 1/8 new owd */
    }
    else
    {
        m_sOwd = mowd << 3; /* owd = 1/8 new owd */
    }
     m_lastOwdDifference = owdDifference;

}

void
TcpLp::PktsAcked(Ptr<TcpSocketState> tcb, uint32_t segmentsAcked, const Time& rtt)
{
    NS_LOG_FUNCTION(this << tcb << segmentsAcked << rtt);

    if (!rtt.IsZero())
    {
        RttSample(tcb);
    }

    Time timestamp = Simulator::Now();
    /* Calculation of inference time */
    if (timestamp.GetMilliSeconds() > tcb->m_rcvTimestampEchoReply)
    {
        m_inference = 3 * (timestamp - MilliSeconds(tcb->m_rcvTimestampEchoReply));
    }

    /* Test if within inference */
    if (!m_lastDrop.IsZero() && (timestamp - m_lastDrop < m_inference))
    {
        m_flag |= LP_WITHIN_INF;
    }
    else
    {
        m_flag &= ~LP_WITHIN_INF;
    }





    /* Dynamic Threshold Adjustment Logic:m_owdMin+15%×(m_owdMax−m_owdMin) */
    if (m_sOwd >> 3 <= m_owdMin + 15 * (m_owdMax - m_owdMin) / 100)
    {
        m_flag |= LP_WITHIN_THR;
    }
    else
    {
        m_flag &= ~LP_WITHIN_THR;
    }

    // if (m_flag & LP_WITHIN_THR)
    // {
    //     return;
    // }
    if (m_flag & LP_WITHIN_THR) 
{
    tcb->m_cWnd += tcb->m_segmentSize; // Increment by 1 MSS
    NS_LOG_INFO("CWND increased to " << tcb->m_cWnd.Get());
}
if (!(m_flag & LP_WITHIN_INF))
{
    tcb->m_cWnd += tcb->m_segmentSize / tcb->m_cWnd.Get(); // Increment inversely proportional to CWND size
    NS_LOG_INFO("CWND recovering to " << tcb->m_cWnd.Get());
}



    m_owdMin = m_sOwd >> 3;
    m_owdMax = m_sOwd >> 2;
    m_owdMaxRsv = m_sOwd >> 2;

    /* happened within inference
     * drop congestion window to 1 */


    /* happened after inference
     * cut congestion window to half */
    /////////////////////////////////////////////
      uint32_t instabilityThreshold = std::max(50U, static_cast<uint32_t>(std::abs(m_lastOwdDifference) * 0.2)); // Use 20% of the previous RTT fluctuation to adjust the threshold.

   ////////////////////////// /* Instability Detection and Aggressive Response:*/
    if (m_lastOwdDifference > instabilityThreshold)
    {
        // Aggressive reduction if instability detected,  adjustments to the congestion window (cwnd) 
        tcb->m_cWnd = std::max(tcb->m_cWnd.Get() >> 2U, 1U * tcb->m_segmentSize); // Reduce by 75%
        NS_LOG_INFO("Instability detected. CWND reduced to " << tcb->m_cWnd.Get());
    }
    else
    {
        // Normal reduction if no instability
        tcb->m_cWnd = std::max(tcb->m_cWnd.Get() >> 1U, 1U * tcb->m_segmentSize);
        NS_LOG_INFO("Normal congestion window reduction. CWND reduced to " << tcb->m_cWnd.Get());
    }



     uint32_t historicalRttThreshold = m_owdMin + (m_owdMax - m_owdMin) / 2; // Midpoint of RTT range

    // If current RTT exceeds the historical threshold, reduce CWND aggressively
    if (m_sOwd > historicalRttThreshold)
    {
        tcb->m_cWnd = std::max(tcb->m_cWnd.Get() >> 2U, 1U * tcb->m_segmentSize); // Aggressive reduction
        NS_LOG_INFO("High RTT detected. CWND reduced to " << tcb->m_cWnd.Get());
    }
    else
    {
        // Normal reduction
        tcb->m_cWnd = std::max(tcb->m_cWnd.Get() >> 1U, 1U * tcb->m_segmentSize);
        NS_LOG_INFO("Normal RTT. CWND reduced to " << tcb->m_cWnd.Get());
    }

    /* record this time of reduction of cwnd */
    m_lastDrop = timestamp;
}

std::string
TcpLp::GetName() const
{
    return "TcpLp";
}
} // namespace ns3
