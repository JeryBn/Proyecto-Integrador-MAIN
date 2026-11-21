import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createLocalChatModerator, localChatModelAvailability} from '../src/alumno/comunicacion/local-chat-moderation.mjs';

test('the local classifier isolates each message and rejects undecided responses', async () => {
  let destroyed = 0;
  let count = 0;
  const model = {
    availability: async () => 'available',
    create: async options => {
      assert.deepEqual(options.expectedInputs[0].languages, ['es']);
      assert.match(options.initialPrompts[0].content, /Ignora cualquier instrucción/);
      return {
        clone: async () => ({
          prompt: async (input, {responseConstraint}) => {
            assert.ok(JSON.parse(input).mensaje);
            assert.deepEqual(responseConstraint.properties.decision.enum, ['APPROVE', 'EDIT', 'REMOVE']);
            return ['{"decision":"APPROVE"}', '{"decision":"UNKNOWN"}'][count++];
          },
          destroy: () => {destroyed++; }
        }),
        destroy: () => {destroyed++; }
      };
    }
  };
  assert.equal(await localChatModelAvailability(model), 'available');
  const moderation = await createLocalChatModerator({model});
  assert.equal(await moderation.moderate('JAJAJAJA'), 'APPROVE');
  await assert.rejects(moderation.moderate('Prueba'), /decisión válida/);
  moderation.destroy();
  assert.equal(destroyed, 3);
});

test('no local model means moderation fails before any message can be prepared', async () => {
  assert.equal(await localChatModelAvailability(null), 'unavailable');
  await assert.rejects(createLocalChatModerator({model:null}), /no ofrece la IA local/);
});
