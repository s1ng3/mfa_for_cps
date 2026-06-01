import client from '../api/client';

// Downloads the SIEM export as a file by fetching the blob and triggering a browser save.
async function download(path, filename) {
  const res = await client.get(path, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export const siemApi = {
  exportJson: () => download('/siem/export/json', 'siem-audit-export.json'),
  exportCsv: () => download('/siem/export/csv', 'siem-audit-export.csv'),
};
