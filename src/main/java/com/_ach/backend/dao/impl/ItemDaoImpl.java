package com._ach.backend.dao.impl;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.dao.ItemDao;
import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class ItemDaoImpl implements ItemDao {

        private ItemRepository itemRepository;

        public ItemDaoImpl(ItemRepository itemRepository) {
            this.itemRepository = itemRepository;
        }

        @Override
        public ItemRepresentation create(ItemRepresentation item) {
             itemRepository.save(new Item());
            return null;
        }

        @Override
        public Optional<ItemRepresentation> findById(Long id) {
             itemRepository.findById(id);
            return null;
        }

        @Override
        public List<ItemRepresentation> findAll(Pageable pageable, Map<String, String> filter) {
            return null;
        }

        @Override
        public ItemRepresentation update(ItemRepresentation item) {
            if (!itemRepository.existsById(item.getId())) {
                throw new IllegalArgumentException("Item with id " + item.getId() + " does not exist");
            }
             itemRepository.save(new Item());
            return null;
        }

        @Override
        public void delete(Long id) {
            itemRepository.deleteById(id);
        }

        @Override
        public boolean exists(Long id) {
            return itemRepository.existsById(id);
        }

        @Override
        public long count() {
            return itemRepository.count();
        }
    }
