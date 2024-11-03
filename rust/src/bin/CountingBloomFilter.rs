use std::collections::HashSet;
use std::hash::Hash;
use std::time::Instant;
use uuid::Uuid;

pub struct CountingBloomFilter {
    counters: Vec<u32>,
    size: usize,
    num_hash_functions: usize,
}

impl CountingBloomFilter {
    pub fn new(size: usize, num_hash_functions: usize) -> Self {
        CountingBloomFilter {
            counters: vec![0; size],
            size,
            num_hash_functions,
        }
    }

    pub fn add<T: Hash>(&mut self, item: &T) {
        for i in 0..self.num_hash_functions {
            let index = self.get_hash(item, i);
            if self.counters[index] < u32::MAX {
                self.counters[index] += 1;
            }
        }
    }

    pub fn remove<T: Hash>(&mut self, item: &T) {
        for i in 0..self.num_hash_functions {
            let index = self.get_hash(item, i);
            if self.counters[index] > 0 {
                self.counters[index] -= 1;
            }
        }
    }

    pub fn might_contain<T: Hash>(&self, item: &T) -> bool {
        (0..self.num_hash_functions).all(|i| self.counters[self.get_hash(item, i)] > 0)
    }

    fn get_hash<T: Hash>(&self, item: &T, i: usize) -> usize {
        use std::hash::{Hash, Hasher};
        use std::collections::hash_map::DefaultHasher;

        let mut hasher = DefaultHasher::new();
        item.hash(&mut hasher);
        i.hash(&mut hasher);
        let hash = hasher.finish();

        let bytes = hash.to_le_bytes();
        let digest = md5::compute(bytes);
        let result = u64::from_le_bytes(digest[..8].try_into().unwrap());
        (result as usize) % self.size
    }

    pub fn get_estimated_false_positive_rate(&self, num_items: usize) -> f64 {
        (1.0 - (-((self.num_hash_functions as f64) * (num_items as f64) / (self.size as f64))).exp())
            .powi(self.num_hash_functions as i32)
    }
}

fn main() {
    let size = 1_000_000;
    let num_hash_functions = 5;
    let mut filter = CountingBloomFilter::new(size, num_hash_functions);

    let num_items = 100_000;
    let mut added_items = HashSet::new();

    let start_time = Instant::now();
    for _ in 0..num_items {
        let item = Uuid::new_v4();
        filter.add(&item);
        added_items.insert(item);
    }
    let end_time = Instant::now();
    println!(
        "Time to add {} items: {:.2} ms",
        num_items,
        end_time.duration_since(start_time).as_nanos() as f64 / 1e6
    );

    let num_tests = 1_000_000;
    let mut false_positives = 0;

    let start_time = Instant::now();
    for _ in 0..num_tests {
        let test_item = Uuid::new_v4();
        if filter.might_contain(&test_item) && !added_items.contains(&test_item) {
            false_positives += 1;
        }
    }
    let end_time = Instant::now();
    println!(
        "Time to perform {} lookups: {:.2} ms",
        num_tests,
        end_time.duration_since(start_time).as_nanos() as f64 / 1e6
    );

    let actual_fp_rate = false_positives as f64 / num_tests as f64;
    let estimated_fp_rate = filter.get_estimated_false_positive_rate(num_items);
    println!(
        "False positive rate - Actual: {:.6}, Estimated: {:.6}",
        actual_fp_rate, estimated_fp_rate
    );

    let num_removals = num_items / 2;
    let start_time = Instant::now();
    let mut iterator = added_items.iter();
    let mut removed_items = HashSet::new();
    for _ in 0..num_removals {
        if let Some(item) = iterator.next() {
            filter.remove(item);
            removed_items.insert(*item);
        }
    }
    let end_time = Instant::now();
    println!(
        "Time to remove {} items: {:.2} ms",
        num_removals,
        end_time.duration_since(start_time).as_nanos() as f64 / 1e6
    );

    let mut false_negatives = 0;
    for item in &removed_items {
        if filter.might_contain(item) {
            false_negatives += 1;
        }
    }
    println!(
        "False negatives after removal: {} ({:.6}%)",
        false_negatives,
        false_negatives as f64 / num_removals as f64 * 100.0
    );
}
