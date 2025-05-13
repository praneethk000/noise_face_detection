const source = new EventSource('/events');
source.onmessage = function(event) {
    const data = JSON.parse(event.data);
    document.getElementById('peopleCount').textContent = data.peopleCount;
    document.getElementById('noiseLevel').textContent = data.noiseLevel.toFixed(4);
    const noiseStatus = document.getElementById('noiseStatus');
    if (data.isNoisy) {
        noiseStatus.textContent = 'Too Noisy!';
        noiseStatus.className = 'badge bg-danger';
    } else {
        noiseStatus.textContent = 'Quiet';
        noiseStatus.className = 'badge bg-success';
    }
    console.log('Received detection data:', data); // Debug
};
source.onerror = function() {
    console.error('SSE error, retrying...');
    document.getElementById('noiseStatus').textContent = 'Error';
    document.getElementById('noiseStatus').className = 'badge bg-warning';
};