const OriginDate = window.Date;
class MockDate extends OriginDate {
  static baseTime = "%{baseTime}";
  static currentTimeStamp = (MockDate.baseTime === '%{baseTime}' ? new OriginDate() : new OriginDate(MockDate.baseTime)).getTime();
  static originalNow = OriginDate.now();

  constructor(...args) {
    const params = (args && args.length) ? args : [(MockDate.currentTimeStamp + MockDate.getTick())]
    super(...params)
  }

  static [Symbol.hasInstance](instance) {
    return (instance instanceof Date) && typeof instance.getDate === 'function';
  }

  static getTick() {
    return OriginDate.now() - MockDate.originalNow;
  }

  static now() {
    return MockDate.currentTimeStamp + MockDate.getTick()
  }

  static seed(dateTime) {
    console.info(`Seed mocktime to ${dateTime} (${typeof dateTime})`)
    MockDate.currentTimeStamp = (new OriginDate(dateTime)).getTime()
  }

  static reset() {
    MockDate.seed(new OriginDate())
  }
}
window.Date = MockDate;
