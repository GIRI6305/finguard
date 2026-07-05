import React, { useMemo, useState } from 'react'

const PAGE_SIZE = 10

export default function TransactionsTable({ transactions }) {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [riskFilter, setRiskFilter] = useState('ALL')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [sortBy, setSortBy] = useState('timestamp')
  const [sortDir, setSortDir] = useState('desc')
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    let rows = transactions

    if (search.trim()) {
      const q = search.trim().toLowerCase()
      rows = rows.filter((t) =>
        t.cardNumber?.toLowerCase().includes(q) ||
        t.merchant?.toLowerCase().includes(q) ||
        t.location?.toLowerCase().includes(q) ||
        t.transactionId?.toLowerCase().includes(q)
      )
    }

    if (statusFilter !== 'ALL') {
      rows = rows.filter((t) => t.status === statusFilter)
    }

    if (riskFilter === 'HIGH') {
      rows = rows.filter((t) => t.riskScore >= 70)
    } else if (riskFilter === 'LOW') {
      rows = rows.filter((t) => t.riskScore < 40)
    }

    if (dateFrom) {
      rows = rows.filter((t) => new Date(t.timestamp) >= new Date(dateFrom))
    }
    if (dateTo) {
      rows = rows.filter((t) => new Date(t.timestamp) <= new Date(dateTo + 'T23:59:59'))
    }

    const sorted = [...rows].sort((a, b) => {
      let cmp = 0
      if (sortBy === 'timestamp') cmp = new Date(a.timestamp) - new Date(b.timestamp)
      else if (sortBy === 'amount') cmp = a.amount - b.amount
      else if (sortBy === 'riskScore') cmp = a.riskScore - b.riskScore
      return sortDir === 'asc' ? cmp : -cmp
    })

    return sorted
  }, [transactions, search, statusFilter, riskFilter, dateFrom, dateTo, sortBy, sortDir])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageRows = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const toggleSort = (field) => {
    if (sortBy === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc')
    } else {
      setSortBy(field)
      setSortDir('desc')
    }
    setPage(1)
  }

  const inputStyle = { padding: 6, fontSize: 13 }
  const arrow = (field) => (sortBy === field ? (sortDir === 'asc' ? ' ▲' : ' ▼') : '')

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 10, flexWrap: 'wrap' }}>
        <input
          style={{ ...inputStyle, flex: '1 1 200px' }}
          placeholder="Search by card, merchant, location, or transaction ID"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1) }}
        />
        <select style={inputStyle} value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(1) }}>
          <option value="ALL">All statuses</option>
          <option value="APPROVED">Safe (Approved)</option>
          <option value="FLAGGED">Flagged</option>
          <option value="BLOCKED">Blocked (Fraud)</option>
          <option value="PENDING">Pending</option>
        </select>
        <select style={inputStyle} value={riskFilter} onChange={(e) => { setRiskFilter(e.target.value); setPage(1) }}>
          <option value="ALL">All risk levels</option>
          <option value="HIGH">High risk (70+)</option>
          <option value="LOW">Low risk (&lt;40)</option>
        </select>
        <input
          style={inputStyle}
          type="date"
          value={dateFrom}
          onChange={(e) => { setDateFrom(e.target.value); setPage(1) }}
          title="From date"
        />
        <input
          style={inputStyle}
          type="date"
          value={dateTo}
          onChange={(e) => { setDateTo(e.target.value); setPage(1) }}
          title="To date"
        />
      </div>

      <p style={{ fontSize: 12, color: '#888', margin: '0 0 8px' }}>
        Showing {pageRows.length} of {filtered.length} matching transactions
        {filtered.length !== transactions.length && ` (filtered from ${transactions.length} total)`}
      </p>

      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '2px solid #ddd' }}>
            <th style={{ padding: 6 }}>Card</th>
            <th style={{ padding: 6, cursor: 'pointer' }} onClick={() => toggleSort('amount')}>
              Amount{arrow('amount')}
            </th>
            <th style={{ padding: 6 }}>Merchant</th>
            <th style={{ padding: 6 }}>Location</th>
            <th style={{ padding: 6 }}>Status</th>
            <th style={{ padding: 6, cursor: 'pointer' }} onClick={() => toggleSort('riskScore')}>
              Risk{arrow('riskScore')}
            </th>
            <th style={{ padding: 6, cursor: 'pointer' }} onClick={() => toggleSort('timestamp')}>
              Date{arrow('timestamp')}
            </th>
          </tr>
        </thead>
        <tbody>
          {pageRows.length === 0 && (
            <tr><td colSpan="7" style={{ padding: 6, color: '#888' }}>
              {transactions.length === 0 ? 'No transactions yet. Submit your first one above.' : 'No transactions match your search/filters.'}
            </td></tr>
          )}
          {pageRows.map((t) => (
            <tr key={t.id} style={{ borderBottom: '1px solid #eee' }}>
              <td style={{ padding: 6 }}>{t.cardNumber}</td>
              <td style={{ padding: 6 }}>{t.amount}</td>
              <td style={{ padding: 6 }}>{t.merchant}</td>
              <td style={{ padding: 6 }}>{t.location}</td>
              <td style={{ padding: 6 }}>{t.status}</td>
              <td style={{ padding: 6 }}>{Math.round(t.riskScore)}</td>
              <td style={{ padding: 6, fontSize: 11, color: '#666' }}>
                {new Date(t.timestamp).toLocaleString()}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 12, fontSize: 13 }}>
          <button disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Previous</button>
          <span>Page {currentPage} of {totalPages}</span>
          <button disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)}>Next</button>
        </div>
      )}
    </div>
  )
}
