const iterations = 100000;
const date = new Date();

console.time('toLocaleTimeString');
for (let i = 0; i < iterations; i++) {
  date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
console.timeEnd('toLocaleTimeString');

const formatter = new Intl.DateTimeFormat([], { hour: '2-digit', minute: '2-digit' });
console.time('Intl.DateTimeFormat');
for (let i = 0; i < iterations; i++) {
  formatter.format(date);
}
console.timeEnd('Intl.DateTimeFormat');
