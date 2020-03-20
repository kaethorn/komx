import { AsyncSubject } from 'rxjs';

export interface Store {
  name: string;
  options?: object;
  indices?: [string, string, object?][];
}

/**
 * Convenience wrapper around indexedDB.
 */
export class IndexedDb {

  public ready: AsyncSubject<void> = new AsyncSubject<void>();
  private db: IDBDatabase;

  constructor(name: string, version: number, stores: Store[], indexedDb: IDBFactory = window.indexedDB) {
    this.open(name, version, stores, indexedDb);
  }

  public hasKey(storeName: string, key: IDBValidKey): Promise<boolean> {
    return new Promise(resolve => {
      if (!this.db) {
        return resolve(false);
      }
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readonly');
      transaction.onerror = (): void => resolve(false);
      transaction.onabort = (): void => resolve(false);
      const store = transaction.objectStore(storeName).getKey(key);
      store.onerror = (): void => resolve(false);
      store.onsuccess = (event: any): void => {
        resolve(event.target.result === key);
      };
    });
  }

  public get(storeName: string, key: IDBValidKey): Promise<any> {
    return new Promise((resolve, reject) => {
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readonly');
      transaction.onerror = (): void => reject();
      transaction.onabort = (error): void => reject(error);
      const store = transaction.objectStore(storeName).get(key);
      store.onerror = (): void => reject();
      store.onsuccess = (event: any): void => {
        if (event.target.result) {
          resolve(event.target.result);
        } else {
          reject();
        }
      };
    });
  }

  public getAll(storeName: string): Promise<any> {
    return new Promise((resolve, reject) => {
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readonly');
      transaction.onerror = (): void => reject();
      transaction.onabort = (error): void => reject(error);
      const request: IDBRequest = transaction.objectStore(storeName).getAll();
      request.onerror = (): void => reject();
      request.onsuccess = (event: any): void => resolve(event.target.result);
    });
  }

  public getAllBy(storeName: string, key: string, value: any): Promise<any> {
    return new Promise((resolve, reject) => {
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readonly');
      transaction.onerror = (): void => resolve([]);
      transaction.onabort = (error): void => reject(error);
      const index: IDBIndex = transaction.objectStore(storeName).index(key);
      const request: IDBRequest = index.getAll(value);
      request.onerror = (): void => resolve([]);
      request.onsuccess = (event: any): void => resolve(event.target.result);
    });
  }

  public save(storeName: string, item: any, key?: IDBValidKey): Promise<Event> {
    return new Promise((resolve, reject) => {
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readwrite');
      transaction.oncomplete = resolve;
      transaction.onabort = (error): void => reject(error);
      transaction.onerror = (error): void => reject(error);
      const store = transaction.objectStore(storeName).put(item, key);
      store.onerror = (error): void => reject(error);
    });
  }

  public delete(storeName: string, key: string): Promise<Event> {
    return new Promise((resolve, reject) => {
      const transaction: IDBTransaction = this.db.transaction([storeName], 'readwrite');
      transaction.oncomplete = resolve;
      transaction.onerror = (error): void => reject(error);
      transaction.onabort = (error): void => reject(error);
      const store = transaction.objectStore(storeName).delete(key);
      store.onerror = (error): void => reject(error);
    });
  }

  private open(name: string, version: number, stores: Store[], indexedDb: IDBFactory): Promise<void> {
    return new Promise((resolve, reject) => {
      const request: IDBOpenDBRequest = indexedDb.open(name, version);
      request.onerror = (event): void => {
        console.error(`Error opening DB '${ name }': ${ event }.`);
        reject();
        this.ready.thrownError();
      };
      request.onsuccess = (): void => {
        this.db = request.result;
        resolve();
        this.ready.complete();
      };
      request.onupgradeneeded = (event: any): void => {
        const db: IDBDatabase = event.target.result;
        stores.forEach(store => {
          const objectStore = db.createObjectStore(store.name, store.options);
          if (store.indices) {
            store.indices.forEach(index => {
              objectStore.createIndex(...index);
            });
          }
        });
      };
    });
  }
}
