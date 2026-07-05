import React from 'react'

export default function AlertCard({ alert, isAdmin, onReview }) {
  const isHighRisk = alert.riskScore >= 70
  const bgColor = isHighRisk ? '#ffe3e3' : '#fff8e1'
  const borderColor = isHighRisk ? '#e03131' : '#f08c00'
  const isOpen = alert.status === 'OPEN'

  return (
    <div
      style={{
        background: bgColor,
        borderLeft: `4px solid ${borderColor}`,
        padding: 12,
        marginBottom: 8,
        borderRadius: 4
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontWeight: 'bold' }}>
          {isHighRisk ? 'BLOCKED' : 'FLAGGED'} — risk score {Math.round(alert.riskScore)}
        </div>
        <span style={{ fontSize: 11, color: '#666', fontStyle: 'italic' }}>{alert.status}</span>
      </div>
      <div style={{ fontSize: 13, color: '#444' }}>{alert.reason}</div>
      <div style={{ fontSize: 11, color: '#888' }}>Tx: {alert.transactionId}</div>

      {isAdmin && isOpen && (
        <div style={{ marginTop: 8, display: 'flex', gap: 6 }}>
          <button style={{ fontSize: 12, padding: '4px 8px' }} onClick={() => onReview(alert.id, 'REVIEWED')}>
            Mark Reviewed
          </button>
          <button style={{ fontSize: 12, padding: '4px 8px' }} onClick={() => onReview(alert.id, 'DISMISSED')}>
            Dismiss
          </button>
        </div>
      )}
    </div>
  )
}
